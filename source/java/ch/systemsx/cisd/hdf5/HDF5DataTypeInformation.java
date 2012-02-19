/*
 * Copyright 2007 ETH Zuerich, CISD.
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

import org.apache.commons.lang.ObjectUtils;

import ch.systemsx.cisd.base.mdarray.MDArray;

/**
 * A class that holds relevant information about a data type.
 * 
 * @author Bernd Rinn
 */
public final class HDF5DataTypeInformation
{
    /**
     * An object that represents the options for a data type information object.
     * 
     * @author Bernd Rinn
     */
    public static final class DataTypeInfoOptions
    {
        static final DataTypeInfoOptions MINIMAL = new DataTypeInfoOptions(false, false);

        static final DataTypeInfoOptions ALL = new DataTypeInfoOptions(true, true);

        static final DataTypeInfoOptions DEFAULT = new DataTypeInfoOptions(false, true);
        
        static final DataTypeInfoOptions PATH = new DataTypeInfoOptions(true, false);

        private boolean knowsDataTypePath;

        private boolean knowsDataTypeVariant;

        DataTypeInfoOptions(boolean knowsDataTypePath, boolean knowsDataTypeVariant)
        {
            this.knowsDataTypePath = knowsDataTypePath;
            this.knowsDataTypeVariant = knowsDataTypeVariant;
        }

        DataTypeInfoOptions()
        {
            knowsDataTypePath = false;
            knowsDataTypeVariant = true;
        }

        public DataTypeInfoOptions path(boolean readDataTypePath)
        {
            this.knowsDataTypePath = readDataTypePath;
            return this;
        }

        public DataTypeInfoOptions path()
        {
            this.knowsDataTypePath = true;
            return this;
        }

        public DataTypeInfoOptions variant(boolean readDataTypeVariant)
        {
            this.knowsDataTypeVariant = readDataTypeVariant;
            return this;
        }

        public DataTypeInfoOptions noVariant()
        {
            this.knowsDataTypeVariant = false;
            return this;
        }

        public DataTypeInfoOptions all()
        {
            this.knowsDataTypePath = true;
            this.knowsDataTypeVariant = true;
            return this;
        }

        public DataTypeInfoOptions nothing()
        {
            this.knowsDataTypePath = false;
            this.knowsDataTypeVariant = false;
            return this;
        }

        public boolean knowsDataTypePath()
        {
            return knowsDataTypePath;
        }

        public boolean knowsDataTypeVariant()
        {
            return knowsDataTypeVariant;
        }

    }

    /**
     * Returns a new {@link DataTypeInfoOptions} object.
     */
    public static DataTypeInfoOptions options()
    {
        return new DataTypeInfoOptions();
    }

    private final HDF5DataClass dataClass;

    private final boolean arrayType;

    private final String dataTypePathOrNull;

    private final String nameOrNull;

    private int elementSize;

    private int numberOfElements;

    private CharacterEncoding encoding;

    private int[] dimensions;

    private String opaqueTagOrNull;

    private final DataTypeInfoOptions options;

    private HDF5DataTypeVariant typeVariantOrNull;

    HDF5DataTypeInformation(String dataTypePathOrNull, DataTypeInfoOptions options,
            HDF5DataClass dataClass, int elementSize)
    {
        this(dataTypePathOrNull, options, dataClass, CharacterEncoding.ASCII, elementSize,
                new int[]
                    { 1 }, false, null);
    }

    HDF5DataTypeInformation(String dataTypePathOrNull, DataTypeInfoOptions options,
            HDF5DataClass dataClass, CharacterEncoding encoding, int elementSize)
    {
        this(dataTypePathOrNull, options, dataClass, encoding, elementSize, new int[]
            { 1 }, false, null);
    }

    HDF5DataTypeInformation(HDF5DataClass dataClass, int elementSize)
    {
        this(null, DataTypeInfoOptions.ALL, dataClass, CharacterEncoding.ASCII, elementSize,
                new int[]
                    { 1 }, false, null);
    }

    HDF5DataTypeInformation(HDF5DataClass dataClass, CharacterEncoding encoding, int elementSize)
    {
        this(null, DataTypeInfoOptions.ALL, dataClass, encoding, elementSize, new int[]
            { 1 }, false, null);
    }

    HDF5DataTypeInformation(HDF5DataClass dataClass, int elementSize, int numberOfElements)
    {
        this(null, DataTypeInfoOptions.ALL, dataClass, CharacterEncoding.ASCII, elementSize,
                new int[]
                    { numberOfElements }, false, null);

    }

    HDF5DataTypeInformation(HDF5DataClass dataClass, CharacterEncoding encoding, int elementSize,
            int numberOfElements)
    {
        this(null, DataTypeInfoOptions.ALL, dataClass, encoding, elementSize, new int[]
            { numberOfElements }, false, null);

    }

    HDF5DataTypeInformation(String dataTypePathOrNull, DataTypeInfoOptions options,
            HDF5DataClass dataClass, CharacterEncoding encoding, int elementSize,
            int numberOfElements, String opaqueTagOrNull)
    {
        this(dataTypePathOrNull, options, dataClass, encoding, elementSize, new int[]
            { numberOfElements }, false, opaqueTagOrNull);
    }

    HDF5DataTypeInformation(String dataTypePathOrNull, DataTypeInfoOptions options,
            HDF5DataClass dataClass, CharacterEncoding encoding, int elementSize, int[] dimensions,
            boolean arrayType)
    {
        this(dataTypePathOrNull, options, dataClass, encoding, elementSize, dimensions, arrayType,
                null);

    }

    HDF5DataTypeInformation(String dataTypePathOrNull, DataTypeInfoOptions options,
            HDF5DataClass dataClass, CharacterEncoding encoding, int elementSize, int[] dimensions,
            boolean arrayType, String opaqueTagOrNull)
    {
        if (dataClass == HDF5DataClass.BOOLEAN || dataClass == HDF5DataClass.STRING)
        {
            this.dataTypePathOrNull = null;
            this.nameOrNull = null;
        } else
        {
            this.dataTypePathOrNull = dataTypePathOrNull;
            this.nameOrNull = HDF5Utils.tryGetDataTypeNameFromPath(dataTypePathOrNull, dataClass);
        }
        this.arrayType = arrayType;
        this.dataClass = dataClass;
        this.elementSize = elementSize;
        this.dimensions = dimensions;
        this.numberOfElements = MDArray.getLength(dimensions);
        this.encoding = encoding;
        this.opaqueTagOrNull = opaqueTagOrNull;
        this.options = options;
    }

    /**
     * Returns the data class (<code>INTEGER</code>, <code>FLOAT</code>, ...) of this type.
     */
    public HDF5DataClass getDataClass()
    {
        return dataClass;
    }

    /**
     * Returns the size of one element (in bytes) of this type.
     */
    public int getElementSize()
    {
        return elementSize;
    }

    /**
     * The length that is usable. Usually equals to {@link #getElementSize()}, except for Strings,
     * where it takes into account the terminating '\0' and the character encoding.
     */
    public int getUsableLength()
    {
        if (dataClass == HDF5DataClass.STRING && elementSize > 0)
        {
            return (encoding == CharacterEncoding.UTF8) ? ((elementSize - 1) / 2)
                    : (elementSize - 1);
        } else
        {
            return elementSize;
        }
    }

    void setElementSize(int elementSize)
    {
        this.elementSize = elementSize;
    }

    /**
     * Returns the number of elements of this type.
     * <p>
     * This will be 1 except for array data types.
     */
    public int getNumberOfElements()
    {
        return numberOfElements;
    }

    /**
     * Returns the total size (in bytes) of this data set.
     */
    public int getSize()
    {
        return elementSize * numberOfElements;
    }

    /**
     * Returns the dimensions along each axis of this type.
     */
    public int[] getDimensions()
    {
        return dimensions;
    }

    void setDimensions(int[] dimensions)
    {
        this.dimensions = dimensions;
        this.numberOfElements = MDArray.getLength(dimensions);
    }

    /**
     * Returns <code>true</code> if this type is an HDF5 array type.
     */
    public boolean isArrayType()
    {
        return arrayType;
    }

    /**
     * Returns <code>true</code> if this type is an HDF5 VL (variable-length) type.
     */
    public boolean isVariableLengthType()
    {
        return elementSize < 0;
    }

    /**
     * Returns the tag of an opaque data type, or <code>null</code>, if this data type is not
     * opaque.
     */
    public String tryGetOpaqueTag()
    {
        return opaqueTagOrNull;
    }

    /**
     * Returns whether the data type path has been determined.
     * <p>
     * A return value of <code>true</code> does <i>not necessarily</i> mean that
     * {@link #tryGetDataTypePath()} will return a value other than <code>null</code>, but a return
     * value of <code>false</code> means that this method will always return <code>null</code>.
     */
    public boolean knowsDataTypePath()
    {
        return options.knowsDataTypePath();
    }

    /**
     * If this is a committed (named) data type and {@link #knowsDataTypePath()} ==
     * <code>true</code>, return the path of the data type. Otherwise <code>null</code> is returned.
     */
    public String tryGetDataTypePath()
    {
        return dataTypePathOrNull;
    }

    /**
     * Returns the name of this datatype, if it is a committed data type.
     */
    public String tryGetName()
    {
        return nameOrNull;
    }

    /**
     * Returns whether the data type variant has been determined.
     * <p>
     * A return value of <code>true</code> does <i>not necessarily</i> mean that
     * {@link #tryGetTypeVariant()} will return a value other than <code>null</code>, but a return
     * value of <code>false</code> means that this method will always return <code>null</code>.
     */
    public boolean knowsDataTypeVariant()
    {
        return options.knowsDataTypeVariant;
    }

    /**
     * Returns the {@link HDF5DataTypeVariant}, or <code>null</code>, if this type has no variant or
     * {@link #knowsDataTypeVariant()} == <code>false</code>.
     */
    public HDF5DataTypeVariant tryGetTypeVariant()
    {
        if (typeVariantOrNull == null && options.knowsDataTypeVariant())
        {
            return HDF5DataTypeVariant.NONE;
        } else
        {
            return typeVariantOrNull;
        }
    }

    private HDF5DataTypeVariant tryGetTypeVariantReplaceNoneWithNull()
    {
        return (typeVariantOrNull == HDF5DataTypeVariant.NONE) ? null : typeVariantOrNull;
    }

    void setTypeVariant(HDF5DataTypeVariant typeVariant)
    {
        this.typeVariantOrNull = typeVariant;
    }

    /**
     * Returns <code>true</code>, if the data set is a time stamp, or <code>false</code> otherwise.
     */
    public boolean isTimeStamp()
    {
        return (typeVariantOrNull != null) ? typeVariantOrNull.isTimeStamp() : false;
    }

    /**
     * Returns <code>true</code>, if the data set is a time duration, or <code>false</code>
     * otherwise.
     */
    public boolean isTimeDuration()
    {
        return (typeVariantOrNull != null) ? typeVariantOrNull.isTimeDuration() : false;
    }

    /**
     * Returns the time unit of the data set, if the data set is a time duration, or
     * <code>null</code> otherwise.
     */
    public HDF5TimeUnit tryGetTimeUnit()
    {
        return (typeVariantOrNull != null) ? typeVariantOrNull.tryGetTimeUnit() : null;
    }

    /**
     * Returns an appropriate Java type, or <code>null</code>, if this HDF5 type has no appropriate
     * Java type.
     */
    public Class<?> tryGetJavaType()
    {
        final int rank = (dimensions.length == 1 && dimensions[0] == 1) ? 0 : dimensions.length;
        final Class<?> overrideDataTypeOrNull =
                HDF5CompoundByteifyerFactory.tryGetOverrideJavaType(dataClass, rank, elementSize,
                        typeVariantOrNull);
        if (overrideDataTypeOrNull != null)
        {
            return overrideDataTypeOrNull;
        } else
        {
            return dataClass.getJavaTypeProvider().tryGetJavaType(rank, elementSize,
                    typeVariantOrNull);
        }
    }

    //
    // Object
    //

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || obj instanceof HDF5DataTypeInformation == false)
        {
            return false;
        }
        final HDF5DataTypeInformation that = (HDF5DataTypeInformation) obj;
        final HDF5DataTypeVariant thisTypeVariant = tryGetTypeVariant();
        final HDF5DataTypeVariant thatTypeVariant = that.tryGetTypeVariant();
        return dataClass.equals(that.dataClass) && elementSize == that.elementSize
                && encoding == that.encoding && numberOfElements == that.numberOfElements
                && ObjectUtils.equals(nameOrNull, that.nameOrNull)
                && ObjectUtils.equals(dataTypePathOrNull, that.dataTypePathOrNull)
                && ObjectUtils.equals(thisTypeVariant, thatTypeVariant);
    }

    @Override
    public int hashCode()
    {
        final HDF5DataTypeVariant typeVariant = tryGetTypeVariant();
        return ((((((17 * 59 + dataClass.hashCode()) * 59 + elementSize) * 59 + encoding.ordinal()) * 59 + numberOfElements) * 59 + ObjectUtils
                .hashCode(nameOrNull)) * 59 + ObjectUtils.hashCode(dataTypePathOrNull) * 59)
                + ObjectUtils.hashCode(typeVariant);
    }

    @Override
    public String toString()
    {
        final String name;
        if (nameOrNull != null)
        {
            name = "<" + nameOrNull + ">";
        } else
        {
            name = "";
        }
        final HDF5DataTypeVariant variantOrNull = tryGetTypeVariantReplaceNoneWithNull();
        if (numberOfElements == 1)
        {
            if (variantOrNull != null)
            {
                return name + dataClass + "(" + getUsableLength() + ")/" + variantOrNull.toString();
            } else
            {
                return name + dataClass + "(" + getUsableLength() + ")";
            }
        } else if (dimensions.length == 1)
        {
            if (variantOrNull != null)
            {
                return name + dataClass + "(" + getUsableLength() + ", #" + numberOfElements + ")/"
                        + variantOrNull.toString();
            } else
            {
                return name + dataClass + "(" + getUsableLength() + ", #" + numberOfElements + ")";
            }
        } else
        {
            final StringBuilder builder = new StringBuilder();
            builder.append(name);
            builder.append(dataClass.toString());
            builder.append('(');
            builder.append(getUsableLength());
            builder.append(", [");
            for (int d : dimensions)
            {
                builder.append(d);
                builder.append(',');
            }
            if (dimensions.length > 0)
            {
                builder.setLength(builder.length() - 1);
            }
            builder.append(']');
            builder.append(')');
            if (typeVariantOrNull != null)
            {
                builder.append('/');
                builder.append(typeVariantOrNull.toString());
            }
            return builder.toString();
        }
    }
}