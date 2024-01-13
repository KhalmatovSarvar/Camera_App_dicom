package com.sandipbhattacharya.cameraapp.helper;

import android.util.Log;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.util.UIDUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

public class MultiFrameJpg2Dcm {
    private static final String TAG = "MultiFrameJpg2Dcm";
    public MultiFrameJpg2Dcm(File[] jpegFiles, File dicomOutputFile) {
        try {
            Log.d(TAG, "Creating MultiFrameJpg2Dcm...");
            // Create a new DICOM file meta information dataset
            Attributes fmi = new Attributes();
            fmi.setString(Tag.ImplementationVersionName, VR.SH, "DCM4CHE3");
            fmi.setString(Tag.ImplementationClassUID, VR.UI, UIDUtils.createUID());
            fmi.setString(Tag.TransferSyntaxUID, VR.UI, UID.JPEGLossless);
            fmi.setString(Tag.MediaStorageSOPClassUID, VR.UI, UID.SecondaryCaptureImageStorage);
            fmi.setString(Tag.MediaStorageSOPInstanceUID, VR.UI, UIDUtils.createUID());
            fmi.setString(Tag.FileMetaInformationVersion, VR.OB, "1");

            // Create a DICOM output stream for writing the DICOM file
            DicomOutputStream dos = new DicomOutputStream(dicomOutputFile);

            // Create a new dataset (header/metadata) for our DICOM image writer
            Attributes dicom = createMultiFrameDicomHeader(jpegFiles.length);

            // Write the dataset to the DICOM output stream
            dos.writeDataset(fmi, dicom);

            // Write the pixel data
            dos.writeHeader(Tag.PixelData, VR.OW, -1);
            dos.writeHeader(Tag.Item, null, 0);

            for (File jpegFile : jpegFiles) {
                writeJpegFrameToDicom(dos, jpegFile);
            }

            // Write sequence delimitation item
            dos.writeHeader(Tag.SequenceDelimitationItem, null, 0);

            // Close the DICOM output stream
            dos.close();
            Log.d(TAG, "MultiFrameJpg2Dcm creation successful.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeJpegFrameToDicom(DicomOutputStream dos, File jpegFile) throws Exception {
        try {
            Log.d(TAG, "Writing JPEG frame to DICOM...");
            int jpgLen = (int) jpegFile.length();

            // Read the JPEG file
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(jpegFile));
            byte[] buffer = new byte[jpgLen];
            bis.read(buffer);

            // Ensure even length and write item delimiter
            if ((jpgLen & 1) != 0) {
                byte[] paddedBuffer = new byte[jpgLen + 1];
                System.arraycopy(buffer, 0, paddedBuffer, 0, jpgLen);
                dos.writeHeader(Tag.Item, null, paddedBuffer.length);
                dos.write(paddedBuffer);
            } else {
                dos.writeHeader(Tag.Item, null, jpgLen);
                dos.write(buffer);
            }

            Log.d(TAG, "Writing JPEG frame to DICOM successful.");

        } catch (Exception e) {
            Log.e(TAG, "Error writing JPEG frame to DICOM: " + e.getMessage());
            throw e; // Rethrow the exception after logging
        }
    }

    public static Attributes createMultiFrameDicomHeader(int numberOfFrames) {
        Log.d(TAG, "Creating MultiFrame DICOM header...");
        // Create a new DICOM dataset (Attributes) for storing information
        Attributes dicom = new Attributes();

        // Add patient related information to the DICOM dataset
        dicom.setString(Tag.PatientName, VR.PN, "Sarvarbek Khalmatov");
        dicom.setString(Tag.PatientID, VR.LO, "1704770926564");
        dicom.setString(Tag.PatientSex, VR.CS, "M");
        dicom.setInt(Tag.PatientAge, VR.IS, 23);

// Add study related information to the DICOM dataset
        dicom.setDate(Tag.StudyDate, VR.DA, new java.util.Date(2023, 5, 24)); // Set the study date (year, month, day)
        dicom.setDate(Tag.StudyTime, VR.TM, new java.util.Date(0, 0, 0, 22, 22, 0)); // Set the study time (hour, minute, second)
        dicom.setString(Tag.StudyDescription, VR.LO, "Multi-Frame Study");

// Add series related information to the DICOM dataset
        dicom.setInt(Tag.SeriesNumber, VR.IS, 1);
        dicom.setDate(Tag.SeriesDate, VR.DA, new java.util.Date());
        dicom.setDate(Tag.SeriesTime, VR.TM, new java.util.Date());
        dicom.setString(Tag.SeriesDescription, VR.LO, "Multi-Frame Series");
        dicom.setString(Tag.Modality, VR.CS, "CT"); // Change modality as needed

        // Set various DICOM attributes
        dicom.setString(Tag.PhotometricInterpretation, VR.CS, "RGB");
        dicom.setInt(Tag.SamplesPerPixel, VR.US, 3);
        dicom.setInt(Tag.PlanarConfiguration, VR.US, 0); // Interleaved planar configuration
        dicom.setInt(Tag.Rows, VR.US, 1920); // Change as needed
        dicom.setInt(Tag.Columns, VR.US, 1080); // Change as needed
        dicom.setInt(Tag.BitsAllocated, VR.US, 8);
        dicom.setInt(Tag.BitsStored, VR.US, 8);
        dicom.setInt(Tag.HighBit, VR.US, 7);
        dicom.setInt(Tag.PixelRepresentation, VR.US, 0);

        // Set unique identifiers for study, series, and instance
        dicom.setString(Tag.StudyInstanceUID, VR.UI, UIDUtils.createUID());
        dicom.setString(Tag.SeriesInstanceUID, VR.UI, UIDUtils.createUID());
        dicom.setString(Tag.SOPInstanceUID, VR.UI, UIDUtils.createUID());

        // Set number of frames
        dicom.setInt(Tag.NumberOfFrames, VR.IS, numberOfFrames);

        // Set Planar Configuration for multi-component pixel data
        Log.d(TAG, "MultiFrame DICOM header creation successful.");
        return dicom;
    }
}

