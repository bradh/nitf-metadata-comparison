package org.codice.nitf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

import org.codice.nitf.filereader.FileType;
import org.codice.nitf.filereader.ImageCompression;
import org.codice.nitf.filereader.ImageCoordinatePair;
import org.codice.nitf.filereader.ImageCoordinatesRepresentation;
import org.codice.nitf.filereader.NitfDataExtensionSegment;
import org.codice.nitf.filereader.NitfFile;
import org.codice.nitf.filereader.NitfImageSegment;
import org.codice.nitf.filereader.NitfSecurityClassification;
import org.codice.nitf.filereader.RasterProductFormatUtilities;
import org.codice.nitf.filereader.Tre;
import org.codice.nitf.filereader.TreCollection;
import org.codice.nitf.filereader.TreEntry;
import org.codice.nitf.filereader.TreGroup;

public class FileComparison
{

    public static void main( String[] args ) {
        if (args.length == 0) {
            System.out.println("No file provided, not comparing");
            return;
        }
        for (String arg : args) {
            if (new File(arg).isDirectory()) {
                System.out.println("Walking contents of " + arg);
                File[] files = new File(arg).listFiles();
                for (File file : files) {
                    handleFile(arg + "/" + file.getName());
                }
            } else {
                handleFile(arg);
            }
        }
    }

    private static void handleFile(String filename) {
        if (new File(filename).isFile() && (! filename.endsWith(".txt"))) {
            System.out.println("Dumping output of " + filename);
            compareOneFile(filename);
        }
    }

    private static void compareOneFile(String filename) {
        new FileComparer(filename);
    }
}
