package eu.esa.giorgio.jpg;

/*
    Copyright (C) 2016 Giorgio Severi

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


public class JpgUtils {

    private final static String METADATA_FILE_NAME = "metadata.txt";
    private final static String AUTH = "AUTHOR:";
    private final static String TITLE = "TITLE:";
    private final static String LAT = "LATITUDE:";
    private final static String LONG = "LONGITUDE:";
    private final static String CAM = "CAMERA:";
    private final static String MODEL = "MODEL:";
    private final static String DATE_ACQ = "DATE_ACQUIRED:";


    private final static int GML = 0;
    private final static int JTS = 1;

    private static Logger logger = Logger.getLogger(JpgUtils.class);

    /*
    Required empty constructor
     */
    private JpgUtils(){}


    /*
    Acquire the file name of the JPEG image used for the preview metadata entry.
     */
    @SuppressWarnings("unused")
    public static String acquireImageName (Object currentFilePath){
        String image = "";
        File folder = new File(currentFilePath.toString());
        if (folder == null)
            return image;

        for( File f : folder.listFiles()){
            if (f.getName().contains(".jpg"))
                image = f.getName();
        }
        logger.debug("Acquired image name = " + image);
        return image;
    }


    /*
    Acquires the sensing date and time from the metadata file. It has to compensate for
    the different date format. Sentinel date has milliseconds precision, Landsat 8 has
    microseconds precision.
     */
    @SuppressWarnings("unused")
    public static String acquireSensingDate (Object currentFilePath){
        String date = "";
        BufferedReader bufferedReader = openFile(computeFilePath(currentFilePath));
        if(bufferedReader == null)
            return date;

        try {
            String line = bufferedReader.readLine();
            while (line != null) {
                String[] words = line.trim().split(" ");
                if (words[0].equalsIgnoreCase(DATE_ACQ))
                    date += words[1];

                line = bufferedReader.readLine();
            }

        } catch (IOException e){
            e.printStackTrace();
        }

        closeFile(bufferedReader);
        logger.debug("--------------- Acquired Date = " + date);
        return date;
    }


    /*
    Acquires the camera brand and model form the metadata file. They will be used as value for the instrument
    metadata field. acquireInstrumentShortName() retrieves only the camera model.
     */
    @SuppressWarnings("unused")
    public static String acquireInstrumentName (Object currentFilePath){
        String [] instrument = acquireInstrumentNameHelper(currentFilePath);
        logger.debug("--------------- Acquired Instrument Name = " + instrument[0]);
        return instrument[0].trim();
    }

    @SuppressWarnings("unused")
    public static String acquireInstrumentShortName (Object currentFilePath){
        String[] instrument = acquireInstrumentNameHelper(currentFilePath);
        logger.debug("--------------- Acquired Instrument Short Name = " + instrument[1]);
        return instrument[1].trim();
    }

    private static String[] acquireInstrumentNameHelper (Object currentFilePath){
        String [] instrument = {"",""};
        int position = 0;
        BufferedReader bufferedReader = openFile(computeFilePath(currentFilePath));
        if(bufferedReader == null)
            return instrument;

        try {
            String line = bufferedReader.readLine();
            while (line != null) {
                String[] words = line.trim().split(" ");
                if (words[0].equalsIgnoreCase(CAM)||words[0].equalsIgnoreCase(MODEL)) {
                    for (int i = 1; i < words.length; i++)
                        instrument[position] += words[i] + " ";
                    position++;
                }
                line = bufferedReader.readLine();
            }

        } catch (IOException e){
            e.printStackTrace();
        }

        closeFile(bufferedReader);
        return instrument;
    }


    /*
    Acquires author name form metadata file.
     */
    @SuppressWarnings("unused")
    public static String acquireAuthorName (Object currentFilePath){
        String author = "";
        BufferedReader bufferedReader = openFile(computeFilePath(currentFilePath));
        if(bufferedReader == null)
            return author;

        try {
            String line = bufferedReader.readLine();
            while (line != null) {
                String[] words = line.trim().split(" ");
                if (words[0].equalsIgnoreCase(AUTH))
                    for (int i = 1; i < words.length; i++)
                        author += words[i] + " ";
                line = bufferedReader.readLine();
            }

        } catch (IOException e){
            e.printStackTrace();
        }

        closeFile(bufferedReader);
        logger.debug("--------------- Acquired Author Name = " + author.trim());
        return author.trim();
    }

    /*
    Search the metadata file for the coordinates of the footprint.
    --------------------------------------------------------------
    JTS Formatting.
    Returns a string containing the couples of coordinates separated by ","; points are separated by " ".
    JTS requires Longitude before Latitude.
    --------------------------------------------------------------
    GML Formatting.
    Returns a string containing the couples of coordinates separated by " "; points are separated by ",".
    GML requires Latitude before Longitude.
    */
    @SuppressWarnings("unused")
    public static String acquireFootprintString (Object currentFilePath, int lib){
        String coords = "";
        String latBuffer = "";
        String longBuffer = "";
        String firstPointBuffer = "";
        int pointCounter = 0;
        String metadataFilePath = currentFilePath.toString() + METADATA_FILE_NAME;
        BufferedReader bufferedReader = openFile(metadataFilePath);
        if(bufferedReader == null)
            return coords;

        try {
            String line = bufferedReader.readLine();
            while (line != null) {
                String[] words = line.trim().split(" ");
                if (words[0].equalsIgnoreCase(LAT))
                    latBuffer = words[1];
                else if (words[0].equalsIgnoreCase(LONG)){
                    longBuffer  = words[1];
                    pointCounter++;
                    if (lib == JTS)
                        coords +=  longBuffer + " " + latBuffer + ",";
                    else // lib == GML
                        coords +=  latBuffer + "," + longBuffer + " ";
                    if (pointCounter == 1)
                        firstPointBuffer = coords;
                }

                line = bufferedReader.readLine();
            }

        } catch (IOException e){
            e.printStackTrace();
        }
        coords += pointApproximation (firstPointBuffer, latBuffer, longBuffer, lib, pointCounter);
        coords = coords.substring (0, coords.length()-1);

        closeFile(bufferedReader);
        logger.debug("--------------- Acquired Coordinates = " + coords);
        return coords;
    }

    /*
    Wrapper for the acquireFootprintString method, used for JTS formatting.
   */
    @SuppressWarnings("unused")
    public static String acquireFootprintStringJTS (Object currentFilePath){
        return acquireFootprintString(currentFilePath, JTS);
    }

    /*
    Wrapper for the acquireFootprintString method, used for GML formatting.
   */
    @SuppressWarnings("unused")
    public static String acquireFootprintStringGML (Object currentFilePath){
        return acquireFootprintString(currentFilePath, GML);
    }


    /*
    WORKAROUND: problems with usage of Point instead of polygon. Generating 2 more points obtained adding 0.001 to
    latitude and longitude respectively.
     */
    private static String pointApproximation (String firstPointBuffer, String latBuffer, String longBuffer, int lib, int pointCounter){
        String newCoords = "";
        if (pointCounter <= 1) {
            String newLatBuffer = latBuffer.substring(0, latBuffer.length() - 1) + (Integer.parseInt(latBuffer.substring(latBuffer.length() - 1)) + 1);
            String newLongBuffer = longBuffer.substring(0, longBuffer.length() - 1) + (Integer.parseInt(longBuffer.substring(longBuffer.length() - 1)) + 1);
            if (lib == JTS) {
                newCoords += newLongBuffer + " " + latBuffer + ",";
                newCoords += longBuffer + " " + newLatBuffer + ",";
            } else { // lib == GML
                newCoords += newLatBuffer + "," + longBuffer + " ";
                newCoords += latBuffer + "," + newLongBuffer + " ";
            }
        }
        return newCoords + firstPointBuffer;
    }


  /*
  Simple file management utilities
   */
    private static BufferedReader openFile (String filename){
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try{
            fileReader = new FileReader(filename);
            bufferedReader = new BufferedReader(fileReader);
        } catch (IOException e){
            e.printStackTrace();
        }
        return bufferedReader;
    }

    private static boolean closeFile(BufferedReader bufferedReader){
        try {
            bufferedReader.close();
        } catch (IOException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static String computeFilePath (Object currentFilePath){
        return currentFilePath.toString() + METADATA_FILE_NAME;
    }


    /*
      Number formatting utilities
       */
    @SuppressWarnings("unused")
    public static String formatNumber(double value){
        logger.debug("----------Input value : "+value);
        return String.format("%.2f",value);
    }

}
