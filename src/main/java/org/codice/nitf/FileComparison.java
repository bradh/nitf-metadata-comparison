package org.codice.nitf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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

import org.codice.nitf.filereader.ImageCompression;
import org.codice.nitf.filereader.ImageCoordinatePair;
import org.codice.nitf.filereader.ImageCoordinatesRepresentation;
import org.codice.nitf.filereader.NitfHeaderReader;
import org.codice.nitf.filereader.NitfImageSegment;
import org.codice.nitf.filereader.NitfSecurityClassification;
import org.codice.nitf.filereader.Tre;
import org.codice.nitf.filereader.TreField;
import org.codice.nitf.filereader.TreListEntry;

public class FileComparison
{
    static final String OUR_OUTPUT_EXTENSION = ".OURS.txt";
    static final String THEIR_OUTPUT_EXTENSION = ".THEIRS.txt";

    public static void main( String[] args )
    {
        if (args.length == 0) {
            System.out.println("No file provided, not comparing");
            return;
        }
        for (String filename : args) {
            System.out.println("Dumping output of " + filename);
            compareOneFile(filename);
        }
    }

    private static void compareOneFile(String filename) {
        generateGdalMetadata(filename);
        generateOurMetadata(filename);
        compareMetadataFiles(filename);
    }

    private static void generateOurMetadata(String filename) {
        NitfHeaderReader header = null;

        try {
            header = new NitfHeaderReader(new FileInputStream(filename));
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        NitfImageSegment segment1 = null;
        if (header.getNumberOfImageSegments() >= 1) {
            segment1 = header.getImageSegment(1);
        }

        BufferedWriter out = null;
        try {
            FileWriter fstream = new FileWriter(filename + OUR_OUTPUT_EXTENSION);
            out = new BufferedWriter(fstream);
            out.write("Driver: NITF/National Imagery Transmission Format\n");
            out.write("Files: " + filename + "\n");
            if (segment1 == null) {
                out.write(String.format("Size is 1, 1\n"));
            } else {
                out.write(String.format("Size is %d, %d\n", segment1.getNumberOfColumns(), segment1.getNumberOfRows()));
            }
            if ((segment1 == null) || (segment1.getImageCoordinatesRepresentation() == ImageCoordinatesRepresentation.NONE)) {
                out.write("Coordinate System is `'\n");
            } else {
                out.write("Coordinate System is:\n");
                out.write("GEOGCS[\"WGS 84\",\n");
                out.write("    DATUM[\"WGS_1984\",\n");
                out.write("        SPHEROID[\"WGS 84\",6378137,298.257223563,\n");
                out.write("            AUTHORITY[\"EPSG\",\"7030\"]],\n");
                out.write("        TOWGS84[0,0,0,0,0,0,0],\n");
                out.write("        AUTHORITY[\"EPSG\",\"6326\"]],\n");
                out.write("    PRIMEM[\"Greenwich\",0,\n");
                out.write("        AUTHORITY[\"EPSG\",\"8901\"]],\n");
                out.write("    UNIT[\"degree\",0.0174532925199433,\n");
                out.write("        AUTHORITY[\"EPSG\",\"9108\"]],\n");
                out.write("    AUTHORITY[\"EPSG\",\"4326\"]]\n");
            }
            out.write("Metadata:\n");
            TreeMap <String, String> metadata = new TreeMap<String, String>();
            metadata.put("NITF_CLEVEL", String.format("%02d", header.getComplexityLevel()));
            metadata.put("NITF_ENCRYP", "0");
            metadata.put("NITF_FBKGC", (String.format("%3d,%3d,%3d",
                        (int)(header.getFileBackgroundColourRed() & 0xFF),
                        (int)(header.getFileBackgroundColourGreen() & 0xFF),
                        (int)(header.getFileBackgroundColourBlue() & 0xFF))));
            metadata.put("NITF_FDT", new SimpleDateFormat("yyyyMMddHHmmss").format(header.getFileDateTime()));
            switch (header.getFileType()) {
                case NSIF_ONE_ZERO:
                    metadata.put("NITF_FHDR", "NSIF01.00");
                    break;
                case NITF_TWO_ZERO:
                    metadata.put("NITF_FHDR", "NITF02.00");
                    break;
                case NITF_TWO_ONE:
                    metadata.put("NITF_FHDR", "NITF02.10");
                    break;
            }
            metadata.put("NITF_FSCATP", header.getFileSecurityMetadata().getClassificationAuthorityType());
            metadata.put("NITF_FSCAUT", header.getFileSecurityMetadata().getClassificationAuthority());
            metadata.put("NITF_FSCLAS", header.getFileSecurityMetadata().getSecurityClassification().getTextEquivalent());
            metadata.put("NITF_FSCLSY", header.getFileSecurityMetadata().getSecurityClassificationSystem());
            metadata.put("NITF_FSCLTX", header.getFileSecurityMetadata().getClassificationText());
            metadata.put("NITF_FSCODE", header.getFileSecurityMetadata().getCodewords());
            metadata.put("NITF_FSCOP", header.getFileSecurityMetadata().getFileCopyNumber());
            metadata.put("NITF_FSCPYS", header.getFileSecurityMetadata().getFileNumberOfCopies());
            metadata.put("NITF_FSCRSN", header.getFileSecurityMetadata().getClassificationReason());
            metadata.put("NITF_FSCTLH", header.getFileSecurityMetadata().getControlAndHandling());
            metadata.put("NITF_FSCTLN", header.getFileSecurityMetadata().getSecurityControlNumber());
            metadata.put("NITF_FSDCDT", header.getFileSecurityMetadata().getDeclassificationDate());
            metadata.put("NITF_FSDCTP", header.getFileSecurityMetadata().getDeclassificationType());
            metadata.put("NITF_FSDCXM", header.getFileSecurityMetadata().getDeclassificationExemption());
            metadata.put("NITF_FSDG", header.getFileSecurityMetadata().getDowngrade());
            metadata.put("NITF_FSDGDT", header.getFileSecurityMetadata().getDowngradeDate());
            metadata.put("NITF_FSREL", header.getFileSecurityMetadata().getReleaseInstructions());
            metadata.put("NITF_FSSRDT", header.getFileSecurityMetadata().getSecuritySourceDate());
            metadata.put("NITF_FTITLE", header.getFileTitle());
            metadata.put("NITF_ONAME", header.getOriginatorsName());
            metadata.put("NITF_OPHONE", header.getOriginatorsPhoneNumber());
            metadata.put("NITF_OSTAID", header.getOriginatingStationId());
            metadata.put("NITF_STYPE", header.getStandardType());
            if (segment1 != null) {
                metadata.put("NITF_ABPP", String.format("%02d", segment1.getActualBitsPerPixelPerBand()));
                metadata.put("NITF_CCS_COLUMN", String.format("%d", segment1.getImageLocationColumn()));
                metadata.put("NITF_CCS_ROW", String.format("%d", segment1.getImageLocationRow()));
                metadata.put("NITF_IALVL", String.format("%d", segment1.getImageAttachmentLevel()));
                metadata.put("NITF_IC", segment1.getImageCompression().getTextEquivalent());
                metadata.put("NITF_ICAT", segment1.getImageCategory().getTextEquivalent());
                if (segment1.getImageCoordinatesRepresentation() == ImageCoordinatesRepresentation.NONE) {
                    metadata.put("NITF_ICORDS", "");
                } else {
                    metadata.put("NITF_ICORDS", segment1.getImageCoordinatesRepresentation().getTextEquivalent());
                }
                metadata.put("NITF_IDATIM", new SimpleDateFormat("yyyyMMddHHmmss").format(segment1.getImageDateTime()));
                metadata.put("NITF_IDLVL", String.format("%d", segment1.getImageDisplayLevel()));
                if (segment1.getImageCoordinatesRepresentation() == ImageCoordinatesRepresentation.DECIMALDEGREES) {
                    metadata.put("NITF_IGEOLO", String.format("%+07.3f%+08.3f%+07.3f%+08.3f%+07.3f%+08.3f%+07.3f%+08.3f", segment1.getImageCoordinates().getCoordinate00().getLatitude(), segment1.getImageCoordinates().getCoordinate00().getLongitude(), segment1.getImageCoordinates().getCoordinate0MaxCol().getLatitude(), segment1.getImageCoordinates().getCoordinate0MaxCol().getLongitude(), segment1.getImageCoordinates().getCoordinateMaxRowMaxCol().getLatitude(), segment1.getImageCoordinates().getCoordinateMaxRowMaxCol().getLongitude(), segment1.getImageCoordinates().getCoordinateMaxRow0().getLatitude(), segment1.getImageCoordinates().getCoordinateMaxRow0().getLongitude()));
                } else if (segment1.getImageCoordinatesRepresentation() == ImageCoordinatesRepresentation.GEOGRAPHIC) {
                    metadata.put("NITF_IGEOLO", String.format("%s%s%s%s", makeGeoString(segment1.getImageCoordinates().getCoordinate00()), makeGeoString(segment1.getImageCoordinates().getCoordinate0MaxCol()), makeGeoString(segment1.getImageCoordinates().getCoordinateMaxRowMaxCol()),
                    makeGeoString(segment1.getImageCoordinates().getCoordinateMaxRow0())));
                }
                metadata.put("NITF_IID1", segment1.getImageIdentifier1());
                metadata.put("NITF_IID2", segment1.getImageIdentifier2());
                metadata.put("NITF_ILOC_COLUMN", String.format("%d", segment1.getImageLocationColumn()));
                metadata.put("NITF_ILOC_ROW", String.format("%d", segment1.getImageLocationRow()));
                metadata.put("NITF_IMAG", String.format("%-4.1f", segment1.getImageMagnification()));
                if (segment1.getNumberOfImageComments() > 0) {
                    StringBuilder commentBuilder = new StringBuilder();
                    for (int i = 0; i < segment1.getNumberOfImageComments(); ++i) {
                        commentBuilder.append(String.format("%-80s", segment1.getImageCommentZeroBase(i)));
                    }
                    metadata.put("NITF_IMAGE_COMMENTS", commentBuilder.toString());
                }
                metadata.put("NITF_IMODE", segment1.getImageMode().getTextEquivalent());
                metadata.put("NITF_IREP", segment1.getImageRepresentation().getTextEquivalent());
                metadata.put("NITF_ISCATP", segment1.getSecurityMetadata().getClassificationAuthorityType());
                metadata.put("NITF_ISCAUT", segment1.getSecurityMetadata().getClassificationAuthority());
                metadata.put("NITF_ISCLAS", segment1.getSecurityMetadata().getSecurityClassification().getTextEquivalent());
                metadata.put("NITF_ISCLSY", segment1.getSecurityMetadata().getSecurityClassificationSystem());
                metadata.put("NITF_ISCLTX", segment1.getSecurityMetadata().getClassificationText());
                metadata.put("NITF_ISCODE", segment1.getSecurityMetadata().getCodewords());
                metadata.put("NITF_ISCRSN", segment1.getSecurityMetadata().getClassificationReason());
                metadata.put("NITF_ISCTLH", segment1.getSecurityMetadata().getControlAndHandling());
                metadata.put("NITF_ISCTLN", segment1.getSecurityMetadata().getSecurityControlNumber());
                metadata.put("NITF_ISDCDT", segment1.getSecurityMetadata().getDeclassificationDate());
                metadata.put("NITF_ISDCTP", segment1.getSecurityMetadata().getDeclassificationType());
                metadata.put("NITF_ISDCXM", segment1.getSecurityMetadata().getDeclassificationExemption());
                metadata.put("NITF_ISDG", segment1.getSecurityMetadata().getDowngrade());
                metadata.put("NITF_ISDGDT", segment1.getSecurityMetadata().getDowngradeDate());
                metadata.put("NITF_ISORCE", segment1.getImageSource());
                metadata.put("NITF_ISREL", segment1.getSecurityMetadata().getReleaseInstructions());
                metadata.put("NITF_ISSRDT", segment1.getSecurityMetadata().getSecuritySourceDate());
                metadata.put("NITF_PJUST", segment1.getPixelJustification().getTextEquivalent());
                metadata.put("NITF_PVTYPE", segment1.getPixelValueType().getTextEquivalent());
                if (segment1.getImageTargetId().length() > 0) {
                    metadata.put("NITF_TGTID", String.format("%17s", segment1.getImageTargetId()));
                } else {
                    metadata.put("NITF_TGTID", "");
                }
                ArrayList<TreListEntry> tres = segment1.getTREsRawStructure();
                for (TreListEntry entry : tres) {
                    for (Tre tre : entry.getTresWithName()) {
                        if (tre.getPrefix() != null) {
                            // if it has a prefix, its probably an old-style NITF metadata field
                            List<TreField> fields = tre.getFields();
                            for (TreField treField: fields) {
                                metadata.put(tre.getPrefix() + treField.getName(), treField.getFieldValue().trim());
                            }
                        }
                    }
                }
            }
            for (String key : metadata.keySet()) {
                out.write(String.format("  %s=%s\n", key, metadata.get(key)));
            }
            if ((segment1 != null) && (segment1.getTREsRawStructure().size() > 0)) {
                out.write("Metadata (xml:TRE):\n");
                out.write("<tres>\n");
                // TODO: iterate over file TREs
                if (segment1 != null) {
                    ArrayList<TreListEntry> tres = segment1.getTREsRawStructure();
                    for (TreListEntry entry : tres) {
                        for (Tre tre : entry.getTresWithName()) {
                            out.write("  <tre name=\"" + tre.getName() + "\" location=\"image\">\n");
                            List<TreField> fields = tre.getFields();
                            for (TreField treField : fields) {
                                if (treField.getFieldValue() != null) {
                                    out.write("    <field name=\"" + treField.getName() + "\" value=\"" + treField.getFieldValue().trim() + "\" />\n");
                                }
                                if (treField.getSubFields() != null) {
                                    System.out.println("TreField: " + treField.getName() + " has " + treField.getSubFields().size() + " subfields");
                                    out.write("    <repeated name=\"" + treField.getName() + "\" number=\"" + treField.getSubFields().size() + "\">\n");
                                    int i = 0;
                                    for (TreField subField : treField.getSubFields()) {
                                        out.write(String.format("      <group index=\"%d\">\n", i));
                                        out.write(String.format("        <field name=\"%s\" value=\"%s\" />\n", subField.getName(), subField.getFieldValue().trim())); 
                                        out.write(String.format("      </group>\n"));
                                        i = i + 1;
                                    }
                                    out.write("    </repeated>\n");
                                }
                            }
                            out.write("  </tre>\n");
                        }
                    }
                }
                out.write("</tres>\n\n");
            }
            if (segment1 != null) {
                switch (segment1.getImageCompression()) {
                    case JPEG:
                    case JPEGMASK:
                        out.write("Image Structure Metadata:\n");
                        out.write("  COMPRESSION=JPEG\n");
                        break;
                    case BILEVEL:
                    case BILEVELMASK:
                    case DOWNSAMPLEDJPEG:
                        out.write("Image Structure Metadata:\n");
                        out.write("  COMPRESSION=BILEVEL\n");
                        break;
                    case LOSSLESSJPEG:
                        out.write("Image Structure Metadata:\n");
                        out.write("  COMPRESSION=LOSSLESS JPEG\n");
                        break;
                }
            }
            if (header.getNumberOfImageSegments() > 1) {
                out.write("Subdatasets:\n");
                for (int i = 0; i < header.getNumberOfImageSegments(); ++i) {
                    out.write(String.format("  SUBDATASET_%d_NAME=NITF_IM:%d:%s\n", i+1, i, filename));
                    out.write(String.format("  SUBDATASET_%d_DESC=Image %d of %s\n", i+1, i+1, filename));
                }
            }
            out.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // This is ugly - feel free to fix it any time.
    private static String makeGeoString(ImageCoordinatePair coords) {
        double latitude = coords.getLatitude();
        double longitude = coords.getLongitude();

        String northSouth = "N";
        if (latitude < 0.0) {
            northSouth = "S";
            latitude = Math.abs(latitude);
        }
        String eastWest = "E";
        if (longitude < 0.0) {
            eastWest = "W";
            longitude = Math.abs(longitude);
        }

        int latDegrees = (int)Math.floor(latitude);
        double minutesAndSecondsPart = (latitude -latDegrees) * 60;
        int latMinutes = (int)Math.floor(minutesAndSecondsPart);
        double secondsPart = (minutesAndSecondsPart - latMinutes) * 60;
        int latSeconds = (int)Math.round(secondsPart);
        if (latSeconds == 60) {
            latMinutes++;
            latSeconds = 0;
        }
        if (latMinutes == 60) {
            latDegrees++;
            latMinutes = 0;
        }
        int lonDegrees = (int)Math.floor(longitude);
        minutesAndSecondsPart = (longitude - lonDegrees) * 60;
        int lonMinutes = (int)Math.floor(minutesAndSecondsPart);
        secondsPart = (minutesAndSecondsPart - lonMinutes) * 60;
        int lonSeconds = (int)Math.round(secondsPart);
        if (lonSeconds == 60) {
            lonMinutes++;
            lonSeconds = 0;
        }
        if (lonMinutes == 60) {
            lonDegrees++;
            lonMinutes = 0;
        }
        return String.format("%02d%02d%02d%s%03d%02d%02d%s", latDegrees, latMinutes, latSeconds, northSouth, lonDegrees, lonMinutes, lonSeconds, eastWest);
    }
    private static void generateGdalMetadata(String filename) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("gdalinfo", "-mdd", "xml:TRE", filename);
            processBuilder.environment().put("NITF_OPEN_UNDERLYING_DS", "NO");
            Process process = processBuilder.start();
            BufferedWriter out = null;
            try {
                FileWriter fstream = new FileWriter(filename + THEIR_OUTPUT_EXTENSION);
                out = new BufferedWriter(fstream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            BufferedReader infoOutputReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            try {
                do {
                    String line = infoOutputReader.readLine();
                    if (line.startsWith("Origin = (")) {
                        continue;
                    }
                    if (line.startsWith("Pixel Size = (")) {
                        continue;
                    }
                    if (line.startsWith("Corner Coordinates:")) {
                        break;
                    }
                    if (line.startsWith("Band 1 Block=")) {
                        break;
                    }
                    out.write(line + "\n");
                } while (infoOutputReader.ready());
                out.close();
            } catch (IOException e) {
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void compareMetadataFiles(String filename) {
        List<String> theirs = fileToLines(filename + THEIR_OUTPUT_EXTENSION);
        List<String> ours  = fileToLines(filename + OUR_OUTPUT_EXTENSION);

        Patch patch = DiffUtils.diff(theirs, ours);

        for (Delta delta: patch.getDeltas()) {
                System.out.println(delta);
        }
        System.out.println("  * Done");
    }

    private static List<String> fileToLines(String filename) {
        List<String> lines = new LinkedList<String>();
        String line = "";
        try {
                BufferedReader in = new BufferedReader(new FileReader(filename));
                while ((line = in.readLine()) != null) {
                        lines.add(line);
                }
        } catch (IOException e) {
                e.printStackTrace();
        }
        return lines;
    }

}
