/*
 * Copyright 2009 ETH Zuerich, CISD
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

import java.io.File;

import ch.systemsx.cisd.common.utilities.OSUtilities;
import ch.systemsx.cisd.hdf5.HDF5WriterConfigurator.FileFormat;

/**
 * If you want the reader to perform numeric conversions, call {@link #performNumericConversions()}
 * before calling {@link #reader()}.
 * 
 * @author Bernd Rinn
 */
public class HDF5ReaderConfigurator
{

    protected final File hdf5File;

    protected boolean performNumericConversions;

    protected HDF5Reader readerWriterOrNull;

    public HDF5ReaderConfigurator(File hdf5File)
    {
        assert hdf5File != null;

        this.hdf5File = hdf5File.getAbsoluteFile();
    }

    /**
     * Returns <code>true</code>, if this platform supports numeric conversions.
     */
    public boolean platformSupportsNumericConversions()
    {
        // On HDF5 1.8.2, numeric conversions on sparcv9 can get us SEGFAULTS for converting between
        // integers and floats.
        if (OSUtilities.getCPUArchitecture().startsWith("sparc"))
        {
            return false;
        }
        return true;
    }

    /**
     * Will try to perform numeric conversions where appropriate if supported by the platform.
     * <p>
     * <strong>Numeric conversions can be platform dependent and are not available on all platforms.
     * Be advised not to rely on numeric conversions if you can help it!</strong>
     */
    public HDF5ReaderConfigurator performNumericConversions()
    {
        if (platformSupportsNumericConversions() == false)
        {
            return this;
        }
        this.performNumericConversions = true;
        return this;
    }

    /**
     * Returns an {@link HDF5Reader} based on this configuration.
     */
    public HDF5Reader reader()
    {
        if (readerWriterOrNull == null)
        {
            readerWriterOrNull =
                    new HDF5Reader(new HDF5BaseReader(hdf5File, performNumericConversions,
                            FileFormat.ALLOW_1_8, false));
        }
        return readerWriterOrNull;
    }

}