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
import java.util.LinkedList;
import java.util.List;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

import org.codice.nitf.filereader.ImageCoordinatePair;
import org.codice.nitf.filereader.ImageCoordinatesRepresentation;
import org.codice.nitf.filereader.NitfHeaderReader;
import org.codice.nitf.filereader.NitfImageSegment;
import org.codice.nitf.filereader.NitfSecurityClassification;

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
            out.write(String.format("Size is %d, %d\n", segment1.getNumberOfColumns(), segment1.getNumberOfRows()));
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
            out.write("Metadata:\n");
            out.write(String.format("  NITF_ABPP=%02d\n", segment1.getActualBitsPerPixelPerBand()));
            out.write(String.format("  NITF_CCS_COLUMN=%d\n", segment1.getImageLocationColumn()));
            out.write(String.format("  NITF_CCS_ROW=%d\n", segment1.getImageLocationRow()));
            out.write(String.format("  NITF_CLEVEL=%02d\n", header.getComplexityLevel()));
            out.write(String.format("  NITF_ENCRYP=0\n"));
            out.write(String.format("  NITF_FBKGC=%3d,%3d,%3d\n",
                        (int)(header.getFileBackgroundColourRed() & 0xFF),
                        (int)(header.getFileBackgroundColourGreen() & 0xFF),
                        (int)(header.getFileBackgroundColourBlue() & 0xFF)));
            out.write(String.format("  NITF_FDT=%s\n", new SimpleDateFormat("yyyyMMddHHmmss").format(header.getFileDateTime())));
            switch (header.getFileType()) {
                case NSIF_ONE_ZERO:
                    out.write("  NITF_FHDR=NSIF01.00\n");
                    break;
                case NITF_TWO_ZERO:
                    out.write("  NITF_FHDR=NITF02.00\n");
                    break;
                case NITF_TWO_ONE:
                    out.write("  NITF_FHDR=NITF02.10\n");
                    break;
            }
            out.write(String.format("  NITF_FSCATP=%s\n", header.getFileSecurityMetadata().getClassificationAuthorityType()));
            out.write(String.format("  NITF_FSCAUT=%s\n", header.getFileSecurityMetadata().getClassificationAuthority()));
            out.write(String.format("  NITF_FSCLAS=%s\n", header.getFileSecurityMetadata().getSecurityClassification().getTextEquivalent()));
            out.write(String.format("  NITF_FSCLSY=%s\n", header.getFileSecurityMetadata().getSecurityClassificationSystem()));
            out.write(String.format("  NITF_FSCLTX=%s\n", header.getFileSecurityMetadata().getClassificationText()));
            out.write(String.format("  NITF_FSCODE=%s\n", header.getFileSecurityMetadata().getCodewords()));
            out.write(String.format("  NITF_FSCOP=%s\n", header.getFileSecurityMetadata().getFileCopyNumber()));
            out.write(String.format("  NITF_FSCPYS=%s\n", header.getFileSecurityMetadata().getFileNumberOfCopies()));
            out.write(String.format("  NITF_FSCRSN=%s\n", header.getFileSecurityMetadata().getClassificationReason()));
            out.write(String.format("  NITF_FSCTLH=%s\n", header.getFileSecurityMetadata().getControlAndHandling()));
            out.write(String.format("  NITF_FSCTLN=%s\n", header.getFileSecurityMetadata().getSecurityControlNumber()));
            out.write(String.format("  NITF_FSDCDT=%s\n", header.getFileSecurityMetadata().getDeclassificationDate()));
            out.write(String.format("  NITF_FSDCTP=%s\n", header.getFileSecurityMetadata().getDeclassificationType()));
            out.write(String.format("  NITF_FSDCXM=%s\n", header.getFileSecurityMetadata().getDeclassificationExemption()));
            out.write(String.format("  NITF_FSDG=%s\n", header.getFileSecurityMetadata().getDowngrade()));
            out.write(String.format("  NITF_FSDGDT=%s\n", header.getFileSecurityMetadata().getDowngradeDate()));
            out.write(String.format("  NITF_FSREL=%s\n", header.getFileSecurityMetadata().getReleaseInstructions()));
            out.write(String.format("  NITF_FSSRDT=%s\n", header.getFileSecurityMetadata().getSecuritySourceDate()));
            out.write(String.format("  NITF_FTITLE=%s\n", header.getFileTitle()));
            out.write(String.format("  NITF_IALVL=%d\n", segment1.getImageAttachmentLevel()));
            out.write(String.format("  NITF_IC=%s\n", segment1.getImageCompression().getTextEquivalent()));
            out.write(String.format("  NITF_ICAT=%s\n", segment1.getImageCategory().getTextEquivalent()));
            out.write(String.format("  NITF_ICORDS=%s\n", segment1.getImageCoordinatesRepresentation().getTextEquivalent()));
            out.write(String.format("  NITF_IDATIM=%s\n", new SimpleDateFormat("yyyyMMddHHmmss").format(segment1.getImageDateTime())));
            out.write(String.format("  NITF_IDLVL=%d\n", segment1.getImageDisplayLevel()));
            if (segment1.getImageCoordinatesRepresentation() == ImageCoordinatesRepresentation.DECIMALDEGREES) {
                out.write(String.format("  NITF_IGEOLO=%+07.3f%+08.3f%+07.3f%+08.3f%+07.3f%+08.3f%+07.3f%+08.3f\n", segment1.getImageCoordinates().getCoordinate00().getLatitude(), segment1.getImageCoordinates().getCoordinate00().getLongitude(), segment1.getImageCoordinates().getCoordinate0MaxCol().getLatitude(), segment1.getImageCoordinates().getCoordinate0MaxCol().getLongitude(), segment1.getImageCoordinates().getCoordinateMaxRowMaxCol().getLatitude(), segment1.getImageCoordinates().getCoordinateMaxRowMaxCol().getLongitude(), segment1.getImageCoordinates().getCoordinateMaxRow0().getLatitude(), segment1.getImageCoordinates().getCoordinateMaxRow0().getLongitude()));
            } else if (segment1.getImageCoordinatesRepresentation() == ImageCoordinatesRepresentation.GEOGRAPHIC) {
                out.write("  NITF_IGEOLO=");
                out.write(makeGeoString(segment1.getImageCoordinates().getCoordinate00()));
                out.write(makeGeoString(segment1.getImageCoordinates().getCoordinate0MaxCol()));
                out.write(makeGeoString(segment1.getImageCoordinates().getCoordinateMaxRowMaxCol()));
                out.write(makeGeoString(segment1.getImageCoordinates().getCoordinateMaxRow0()));
                out.write("\n");
            }
            out.write(String.format("  NITF_IID1=%s\n", segment1.getImageIdentifier1()));
            out.write(String.format("  NITF_IID2=%s\n", segment1.getImageIdentifier2()));
            out.write(String.format("  NITF_ILOC_COLUMN=%d\n", segment1.getImageLocationColumn()));
            out.write(String.format("  NITF_ILOC_ROW=%d\n", segment1.getImageLocationRow()));
            out.write(String.format("  NITF_IMAG=%-4.1f\n", segment1.getImageMagnification()));
            out.write(String.format("  NITF_IMODE=%s\n", segment1.getImageMode().getTextEquivalent()));
            out.write(String.format("  NITF_IREP=%s\n", segment1.getImageRepresentation().getTextEquivalent()));
            out.write(String.format("  NITF_ISCATP=%s\n", segment1.getSecurityMetadata().getClassificationAuthorityType()));
            out.write(String.format("  NITF_ISCAUT=%s\n", segment1.getSecurityMetadata().getClassificationAuthority()));
            out.write(String.format("  NITF_ISCLAS=%s\n", segment1.getSecurityMetadata().getSecurityClassification().getTextEquivalent()));
            out.write(String.format("  NITF_ISCLSY=%s\n", segment1.getSecurityMetadata().getSecurityClassificationSystem()));
            out.write(String.format("  NITF_ISCLTX=%s\n", segment1.getSecurityMetadata().getClassificationText()));
            out.write(String.format("  NITF_ISCODE=%s\n", segment1.getSecurityMetadata().getCodewords()));
            out.write(String.format("  NITF_ISCRSN=%s\n", segment1.getSecurityMetadata().getClassificationReason()));
            out.write(String.format("  NITF_ISCTLH=%s\n", segment1.getSecurityMetadata().getControlAndHandling()));
            out.write(String.format("  NITF_ISCTLN=%s\n", segment1.getSecurityMetadata().getSecurityControlNumber()));
            out.write(String.format("  NITF_ISDCDT=%s\n", segment1.getSecurityMetadata().getDeclassificationDate()));
            out.write(String.format("  NITF_ISDCTP=%s\n", segment1.getSecurityMetadata().getDeclassificationType()));
            out.write(String.format("  NITF_ISDCXM=%s\n", segment1.getSecurityMetadata().getDeclassificationExemption()));
            out.write(String.format("  NITF_ISDG=%s\n", segment1.getSecurityMetadata().getDowngrade()));
            out.write(String.format("  NITF_ISDGDT=%s\n", segment1.getSecurityMetadata().getDowngradeDate()));
            out.write(String.format("  NITF_ISORCE=%s\n", segment1.getImageSource()));
            out.write(String.format("  NITF_ISREL=%s\n", segment1.getSecurityMetadata().getReleaseInstructions()));
            out.write(String.format("  NITF_ISSRDT=%s\n", segment1.getSecurityMetadata().getSecuritySourceDate()));
            out.write(String.format("  NITF_ONAME=%s\n", header.getOriginatorsName()));
            out.write(String.format("  NITF_OPHONE=%s\n", header.getOriginatorsPhoneNumber()));
            out.write(String.format("  NITF_OSTAID=%s\n", header.getOriginatingStationId()));
            out.write(String.format("  NITF_PJUST=%s\n", segment1.getPixelJustification().getTextEquivalent()));
            out.write(String.format("  NITF_PVTYPE=%s\n", segment1.getPixelValueType().getTextEquivalent()));
            out.write(String.format("  NITF_STYPE=%s\n", header.getStandardType()));
            // This is some wierdness from GDAL.
            if (segment1.getImageTargetId().length() > 0) {
                out.write(String.format("  NITF_TGTID=%17s\n", segment1.getImageTargetId()));
            } else {
                out.write("  NITF_TGTID=");
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
        int latDegrees = (int)Math.floor(latitude);
        double minutesAndSecondsPart = (latitude -latDegrees) * 60;
        int latMinutes = (int)Math.floor(minutesAndSecondsPart);
        double secondsPart = (minutesAndSecondsPart - latMinutes) * 60;
        int latSeconds = (int)Math.round(secondsPart);

        double longitude = coords.getLongitude();
        int lonDegrees = (int)Math.floor(longitude);
        minutesAndSecondsPart = (longitude - lonDegrees) * 60;
        int lonMinutes = (int)Math.floor(minutesAndSecondsPart);
        secondsPart = (minutesAndSecondsPart - lonMinutes) * 60;
        int lonSeconds = (int)Math.round(secondsPart);

        String northSouth = "N";
        if (latitude < 0.0) {
            northSouth = "S";
        }
        String eastWest = "E";
        if (longitude < 0.0) {
            eastWest = "W";
        }
        return String.format("%02d%02d%02d%s%03d%02d%02d%s", latDegrees, latMinutes, latSeconds, northSouth, lonDegrees, lonMinutes, lonSeconds, eastWest);
    }
    private static void generateGdalMetadata(String filename) {
        try {
            Process process = new ProcessBuilder("gdalinfo", filename).start();
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
                    if (line.startsWith("Corner Coordinates:")) {
                        continue;
                    }
                    if (line.startsWith("Upper Left  (")) {
                        continue;
                    }
                    if (line.startsWith("Upper Right (")) {
                        continue;
                    }
                    if (line.startsWith("Lower Left  (")) {
                        continue;
                    }
                    if (line.startsWith("Lower Right (")) {
                        continue;
                    }
                    if (line.startsWith("Center      (")) {
                        continue;
                    }
                    if (line.startsWith("Band 1 Block=")) {
                        continue;
                    }
                    if (line.startsWith("Origin = (")) {
                        continue;
                    }
                    if (line.startsWith("Pixel Size = (")) {
                        continue;
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
