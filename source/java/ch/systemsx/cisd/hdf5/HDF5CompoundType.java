/*
 * Copyright 2008 ETH Zuerich, CISD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.systemsx.cisd.hdf5;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;

import ncsa.hdf.hdf5lib.exceptions.HDF5JavaException;

/**
 * The definition of a HDF5 compound type. For information on how to create and work with compound
 * types, have a look at {@link IHDF5CompoundInformationRetriever}. The simplest way of creating a
 * compound type for a Java class, is
 * {@link IHDF5CompoundInformationRetriever#getInferredCompoundType(Class)}.
 * <p>
 * Once you have a compound type, you may use methods like
 * {@link IHDF5Reader#readCompound(String, HDF5CompoundType)} and
 * {@link IHDF5Writer#writeCompound(String, HDF5CompoundType, Object)} and to read and write them.
 * 
 * @author Bernd Rinn
 */
public class HDF5CompoundType<T> extends HDF5DataType
{
    interface IHDF5InternalCompoundMemberInformationRetriever
    {
        HDF5CompoundMemberInformation[] getCompoundMemberInformation();
    }

    private final String nameOrNull;

    private final Class<T> compoundType;

    private final HDF5ValueObjectByteifyer<T> objectByteifyer;

    private final IHDF5InternalCompoundMemberInformationRetriever informationRetriever;

    /**
     * Creates a new {@link HDF5CompoundType} for the given <var>compoundType</var> and the mapping
     * defined by <var>members</var>.
     * 
     * @param nameOrNull The name of this type, or <code>null</code>, if it is not known.
     * @param storageTypeId The storage data type id.
     * @param nativeTypeId The native (memory) data type id.
     * @param compoundType The Java type that corresponds to this type.
     * @param objectByteifer The byteifer to use to convert between the Java object and the HDF5
     *            file.
     * @param informationRetriever A role that allows to retrieve compound member information for a
     *            given compound type id.
     */
    HDF5CompoundType(int fileId, int storageTypeId, int nativeTypeId, String nameOrNull,
            Class<T> compoundType, HDF5ValueObjectByteifyer<T> objectByteifer,
            IHDF5InternalCompoundMemberInformationRetriever informationRetriever)
    {
        super(fileId, storageTypeId, nativeTypeId);
        assert compoundType != null;
        assert objectByteifer != null;
        assert informationRetriever != null;

        this.nameOrNull = nameOrNull;
        this.compoundType = compoundType;
        this.objectByteifyer = objectByteifer;
        this.informationRetriever = informationRetriever;
    }

    /**
     * Returns the Java type of the compound.
     */
    public Class<T> getCompoundType()
    {
        return compoundType;
    }

    /**
     * Returns an array with the {@link HDF5CompoundMemberInformation} of all compound members.
     */
    public HDF5CompoundMemberInformation[] getCompoundMemberInformation()
    {
        return informationRetriever.getCompoundMemberInformation();
    }

    /**
     * Returns <code>true</code>, if the mapping between the in-memory and the on-disk
     * representation is incomplete, that is if either {@link #isDiskRepresentationIncomplete()} or
     * {@link #isMemoryRepresentationIncomplete()} returns <code>true</code>.
     */
    public boolean isMappingIncomplete()
    {
        return isMemoryRepresentationIncomplete() || isDiskRepresentationIncomplete();
    }

    /**
     * Returns <code>true</code> if there are compound members in the on-disk representation that
     * are not mapped to fields in the in-memory representation.
     */
    public boolean isMemoryRepresentationIncomplete()
    {
        return objectByteifyer.hasUnmappedMembers();
    }

    /**
     * Returns an array with the names of compound members that are not mapped to the in-memory
     * representation. If no members are unmapped, an empty array is returned.
     */
    public String[] getUnmappedCompoundMemberNames()
    {
        return objectByteifyer.getUnmappedMembers();
    }

    private Map<String, HDF5CompoundMemberInformation> getCompoundMemberInformationMap()
    {
        final Map<String, HDF5CompoundMemberInformation> result =
                new HashMap<String, HDF5CompoundMemberInformation>();
        for (HDF5CompoundMemberInformation info : getCompoundMemberInformation())
        {
            result.put(info.getName(), info);
        }
        return result;
    }

    /**
     * Returns an with the {@link HDF5CompoundMemberInformation} of compound members that are not
     * mapped to the in-memory representation. If no members are unmapped, an empty array is
     * returned.
     */
    public HDF5CompoundMemberInformation[] getUnmappedCompoundMemberInformation()
    {
        final String[] unmappedCompoundMemberNames = getUnmappedCompoundMemberNames();
        if (unmappedCompoundMemberNames.length > 0)
        {
            final Map<String, HDF5CompoundMemberInformation> compoundMemberInfoMap =
                    getCompoundMemberInformationMap();
            final HDF5CompoundMemberInformation[] result =
                    new HDF5CompoundMemberInformation[unmappedCompoundMemberNames.length];
            int idx = 0;
            for (String name : unmappedCompoundMemberNames)
            {
                result[idx++] = compoundMemberInfoMap.get(name);
            }
            return result;
        } else
        {
            return new HDF5CompoundMemberInformation[0];
        }
    }

    /**
     * Returns <code>true</code> if there are fields in the in-memory representation that are not
     * mapped to any compound member in the on-disk representation.
     */
    public boolean isDiskRepresentationIncomplete()
    {
        return getUnmappedFields().isEmpty() == false;
    }

    /**
     * Checks whether the mapping between the on-disk representation and the in-memory
     * representation is complete.
     * 
     * @throws HDF5JavaException if {@link #isMappingIncomplete()} returns <code>true</code>.
     */
    public void checkMappingComplete() throws HDF5JavaException
    {
        final String[] unmappedMembers = getUnmappedCompoundMemberNames();
        final String[] unmappedFields = getUnmappedFieldNames();
        if (unmappedMembers.length > 0 || unmappedFields.length > 0)
        {
            final StringBuilder b = new StringBuilder();
            b.append("Incomplete mapping for compound type '");
            b.append(getName());
            b.append("': ");
            if (unmappedMembers.length > 0)
            {
                b.append("unmapped members: ");
                b.append(ArrayUtils.toString(unmappedMembers));
            }
            if (unmappedMembers.length > 0 && unmappedFields.length > 0)
            {
                b.append(", ");
            }
            if (unmappedFields.length > 0)
            {
                b.append("unmapped fields: ");
                b.append(ArrayUtils.toString(unmappedFields));
            }
            throw new HDF5JavaException(b.toString());
        }
    }

    /**
     * Returns an array with names of fields of the in-memory representation that do not map to any
     * compound member in the on-disk representation.
     */
    public String[] getUnmappedFieldNames()
    {
        final Set<Field> unmappedFields = getUnmappedFields();
        final String[] result = new String[unmappedFields.size()];
        int idx = 0;
        for (Field field : unmappedFields)
        {
            result[idx++] = field.getName();
        }
        return result;
    }

    private Set<Field> getUnmappedFields()
    {
        if (Map.class.isAssignableFrom(compoundType) || List.class.isAssignableFrom(compoundType)
                || compoundType == Object[].class)
        {
            return Collections.emptySet();
        } else
        {
            final Set<Field> fieldSet =
                    new HashSet<Field>(ReflectionUtils.getFieldMap(compoundType).values());
            for (HDF5MemberByteifyer byteiyfer : objectByteifyer.getByteifyers())
            {
                fieldSet.remove(byteiyfer.tryGetField());
            }
            return fieldSet;
        }

    }

    /**
     * Returns the byteifyer to convert between the Java type and the HDF5 type.
     */
    HDF5ValueObjectByteifyer<T> getObjectByteifyer()
    {
        return objectByteifyer;
    }

    @Override
    public String tryGetName()
    {
        return nameOrNull;
    }

    @Override
    public String toString()
    {
        if (nameOrNull != null)
        {
            return "HDF5CompoundType [nameOrNull=" + nameOrNull + ", compoundType="
                    + compoundType.getSimpleName() + ", objectByteifyer=" + objectByteifyer + "]";
        } else
        {
            return "HDF5CompoundType [compoundType=" + compoundType.getSimpleName()
                    + ", objectByteifyer=" + objectByteifyer + "]";
        }
    }

}
