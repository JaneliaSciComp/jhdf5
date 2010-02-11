/*
 * Copyright 2010 ETH Zuerich, CISD
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

import static ch.systemsx.cisd.hdf5.HDF5Utils.getOneDimensionalArraySize;
import static ncsa.hdf.hdf5lib.HDF5Constants.H5T_STRING;

import java.util.Iterator;
import java.util.NoSuchElementException;

import ch.systemsx.cisd.hdf5.HDF5BaseReader.DataSpaceParameters;
import ch.systemsx.cisd.hdf5.cleanup.ICallableWithCleanUp;
import ch.systemsx.cisd.hdf5.cleanup.ICleanUpRegistry;

import ncsa.hdf.hdf5lib.exceptions.HDF5JavaException;

/**
 * The implementation of {@link IHDF5StringReader}.
 *
 * @author Bernd Rinn
 */
public class HDF5StringReader implements IHDF5StringReader
{

    private final HDF5BaseReader baseReader;

    HDF5StringReader(HDF5BaseReader baseReader)
    {
        assert baseReader != null;

        this.baseReader = baseReader;
    }

    //
    // Attributes
    //

    public String getStringAttribute(final String objectPath, final String attributeName)
    {
        assert objectPath != null;
        assert attributeName != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<String> readRunnable = new ICallableWithCleanUp<String>()
            {
                public String call(ICleanUpRegistry registry)
                {
                    final int objectId =
                            baseReader.h5.openObject(baseReader.fileId, objectPath, registry);
                    return baseReader.getStringAttribute(objectId, objectPath, attributeName,
                            registry);
                }
            };
        return baseReader.runner.call(readRunnable);
    }

    //
    // Data Sets
    //

    public String readString(final String objectPath) throws HDF5JavaException
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<String> writeRunnable = new ICallableWithCleanUp<String>()
            {
                public String call(ICleanUpRegistry registry)
                {
                    final int dataSetId =
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    final int dataTypeId =
                            baseReader.h5.getNativeDataTypeForDataSet(dataSetId, registry);
                    final boolean isString = (baseReader.h5.getClassType(dataTypeId) == H5T_STRING);
                    if (isString == false)
                    {
                        throw new HDF5JavaException(objectPath + " needs to be a String.");
                    }
                    if (baseReader.h5.isVariableLengthString(dataTypeId))
                    {
                        String[] data = new String[1];
                        baseReader.h5.readDataSetVL(dataSetId, dataTypeId, data);
                        return data[0];
                    } else
                    {
                        final int size = baseReader.h5.getDataTypeSize(dataTypeId);
                        byte[] data = new byte[size];
                        baseReader.h5.readDataSetNonNumeric(dataSetId, dataTypeId, data);
                        int termIdx;
                        for (termIdx = 0; termIdx < size && data[termIdx] != 0; ++termIdx)
                        {
                        }
                        return new String(data, 0, termIdx);
                    }
                }
            };
        return baseReader.runner.call(writeRunnable);
    }

    public String[] readStringArray(final String objectPath) throws HDF5JavaException
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<String[]> writeRunnable = new ICallableWithCleanUp<String[]>()
            {
                public String[] call(ICleanUpRegistry registry)
                {
                    final int dataSetId =
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    final long[] dimensions = baseReader.h5.getDataDimensions(dataSetId);
                    final String[] data = new String[getOneDimensionalArraySize(dimensions)];
                    final int dataTypeId =
                            baseReader.h5.getNativeDataTypeForDataSet(dataSetId, registry);
                    if (baseReader.h5.isVariableLengthString(dataTypeId))
                    {
                        baseReader.h5.readDataSetVL(dataSetId, dataTypeId, data);
                    } else
                    {
                        final boolean isString =
                                (baseReader.h5.getClassType(dataTypeId) == H5T_STRING);
                        if (isString == false)
                        {
                            throw new HDF5JavaException(objectPath + " needs to be a String.");
                        }
                        baseReader.h5.readDataSetString(dataSetId, dataTypeId, data);
                    }
                    return data;
                }
            };
        return baseReader.runner.call(writeRunnable);
    }

    public String[] readStringArrayBlock(final String objectPath, final int blockSize,
            final long blockNumber)
    {
        return readStringArrayBlockWithOffset(objectPath, blockSize, blockSize * blockNumber);
    }

    public String[] readStringArrayBlockWithOffset(final String objectPath, final int blockSize,
            final long offset)
    {
        assert objectPath != null;

        baseReader.checkOpen();
        final ICallableWithCleanUp<String[]> readCallable = new ICallableWithCleanUp<String[]>()
            {
                public String[] call(ICleanUpRegistry registry)
                {
                    final int dataSetId =
                            baseReader.h5.openDataSet(baseReader.fileId, objectPath, registry);
                    final DataSpaceParameters spaceParams =
                            baseReader.getSpaceParameters(dataSetId, offset, blockSize, registry);
                    final String[] data = new String[spaceParams.blockSize];
                    final int dataTypeId =
                            baseReader.h5.getNativeDataTypeForDataSet(dataSetId, registry);
                    if (baseReader.h5.isVariableLengthString(dataTypeId))
                    {
                        baseReader.h5.readDataSetVL(dataSetId, dataTypeId,
                                spaceParams.memorySpaceId, spaceParams.dataSpaceId, data);
                    } else
                    {
                        final boolean isString =
                                (baseReader.h5.getClassType(dataTypeId) == H5T_STRING);
                        if (isString == false)
                        {
                            throw new HDF5JavaException(objectPath + " needs to be a String.");
                        }
                        baseReader.h5.readDataSetString(dataSetId, dataTypeId,
                                spaceParams.memorySpaceId, spaceParams.dataSpaceId, data);
                    }
                    return data;
                }
            };
        return baseReader.runner.call(readCallable);
    }

    public Iterable<HDF5DataBlock<String[]>> getStringArrayNaturalBlocks(final String dataSetPath)
            throws HDF5JavaException
    {
        baseReader.checkOpen();
        final HDF5DataSetInformation info = baseReader.getDataSetInformation(dataSetPath);
        if (info.getRank() > 1)
        {
            throw new HDF5JavaException("Data Set is expected to be of rank 1 (rank="
                    + info.getRank() + ")");
        }
        final long longSize = info.getDimensions()[0];
        final int size = (int) longSize;
        if (size != longSize)
        {
            throw new HDF5JavaException("Data Set is too large (" + longSize + ")");
        }
        final int naturalBlockSize =
                (info.getStorageLayout() == HDF5StorageLayout.CHUNKED) ? info.tryGetChunkSizes()[0]
                        : size;
        final int sizeModNaturalBlockSize = size % naturalBlockSize;
        final long numberOfBlocks =
                (size / naturalBlockSize) + (sizeModNaturalBlockSize != 0 ? 1 : 0);
        final int lastBlockSize =
                (sizeModNaturalBlockSize != 0) ? sizeModNaturalBlockSize : naturalBlockSize;

        return new Iterable<HDF5DataBlock<String[]>>()
            {
                public Iterator<HDF5DataBlock<String[]>> iterator()
                {
                    return new Iterator<HDF5DataBlock<String[]>>()
                        {
                            long index = 0;

                            public boolean hasNext()
                            {
                                return index < numberOfBlocks;
                            }

                            public HDF5DataBlock<String[]> next()
                            {
                                if (hasNext() == false)
                                {
                                    throw new NoSuchElementException();
                                }
                                final long offset = naturalBlockSize * index;
                                final int blockSize =
                                        (index == numberOfBlocks - 1) ? lastBlockSize
                                                : naturalBlockSize;
                                final String[] block =
                                        readStringArrayBlockWithOffset(dataSetPath, blockSize,
                                                offset);
                                return new HDF5DataBlock<String[]>(block, index++, offset);
                            }

                            public void remove()
                            {
                                throw new UnsupportedOperationException();
                            }
                        };
                }
            };
    }

}