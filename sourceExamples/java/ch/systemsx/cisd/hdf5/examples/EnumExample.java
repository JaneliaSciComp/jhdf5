/*
 * Copyright 2011 ETH Zuerich, CISD
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

package ch.systemsx.cisd.hdf5.examples;

import ch.systemsx.cisd.hdf5.HDF5EnumerationType;
import ch.systemsx.cisd.hdf5.HDF5EnumerationValue;
import ch.systemsx.cisd.hdf5.HDF5EnumerationValueArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;

/**
 * Example for using the HDF5 enum data type.
 */
public class EnumExample
{

    public static void main(String[] args)
    {
        // Write an enum and an enum array
        IHDF5Writer writer = HDF5Factory.configure("enum.h5").writer();
        HDF5EnumerationType enumType = writer.enums().getType("colors", new String[]
            { "RED", "GREEN", "BLUE", "YELLOW", "ORANGE", "MAGENTA", "BLACK" });
        // That is equivalent to 
        // writer.writeEnum("some color", new HDF5EnumerationValue(enumType, 2));
        writer.enums().write("some color", new HDF5EnumerationValue(enumType, "BLUE"));
        writer.enums().writeArray("colors", new HDF5EnumerationValueArray(enumType, new String[]
            { "YELLOW", "MAGENTA", "GREEN" }));
        writer.close();

        // Read an enum and an enum array
        IHDF5Reader reader = HDF5Factory.openForReading("enum.h5");
        System.out.println(reader.enums().readAsString("some color"));
        for (String color : reader.enums().readArray("colors"))
        {
            System.out.println(color);
        }
        reader.close();
    }

}
