package org.fsccell;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.Overlay;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.ChannelSplitter;
import ij.plugin.PlugIn;
import ij.plugin.RGBStackMerge;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.AutoThresholder;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.awt.AWTEvent;
import java.awt.Color;
import java.io.File;

/**
 * FSCell (Fluorescence SmartCell) ImageJ plugin implementation.
 */
public class FSCellPlugin implements PlugIn {

    @Override
    public void run(String arg) {
        GenericDialog modeDialog = new GenericDialog("FSCell");
        String[] modes = {
                "Batch merge & overlap export",
                "Interactive blue nuclei counter"
        };
        modeDialog.addChoice("Mode", modes, modes[0]);
        modeDialog.showDialog();
        if (modeDialog.wasCanceled()) {
            return;
        }
        int modeIndex = modeDialog.getNextChoiceIndex();
        if (modeIndex == 0) {
            runBatchMergeAndExport();
        } else {
            runBlueNucleiCounter();
        }
    }

    private void runBatchMergeAndExport() {
        DirectoryChooser inputChooser = new DirectoryChooser("Select input directory with TIF images");
        String inputDir = inputChooser.getDirectory();
        if (inputDir == null) {
            IJ.showStatus("FSCell: input directory selection cancelled");
            return;
        }
        DirectoryChooser outputChooser = new DirectoryChooser("Select output directory");
        String outputDir = outputChooser.getDirectory();
        if (outputDir == null) {
            IJ.showStatus("FSCell: output directory selection cancelled");
            return;
        }

        File inDir = new File(inputDir);
        File[] tifFiles = inDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".tif") || name.toLowerCase().endsWith(".tiff"));
        if (tifFiles == null || tifFiles.length == 0) {
            IJ.error("FSCell", "No TIF/TIFF files found in the selected directory.");
            return;
        }

        GenericDialog settings = new GenericDialog("Batch settings");
        settings.addNumericField("Blue channel index (1-based)", 1, 0);
        settings.addNumericField("Green channel index (1-based)", 2, 0);
        settings.addNumericField("Red channel index (1-based)", 3, 0);
        settings.addNumericField("Target mean intensity for red/green", 120.0, 1);
        settings.addNumericField("Minimum overlap particle size (pixels)", 25.0, 1);
        settings.addNumericField("Overlap circle radius (pixels)", 6.0, 1);
        settings.showDialog();
        if (settings.wasCanceled()) {
            return;
        }

        int blueIndex = Math.max(1, (int) settings.getNextNumber()) - 1;
        int greenIndex = Math.max(1, (int) settings.getNextNumber()) - 1;
        int redIndex = Math.max(1, (int) settings.getNextNumber()) - 1;
        double targetMean = settings.getNextNumber();
        double minOverlapSize = settings.getNextNumber();
        double circleRadius = settings.getNextNumber();

        IJ.showStatus("FSCell: processing " + tifFiles.length + " files...");
        for (File file : tifFiles) {
            processSingleFile(file, outputDir, blueIndex, greenIndex, redIndex, targetMean, minOverlapSize, circleRadius);
        }
        IJ.showStatus("FSCell: batch processing finished");
    }

    private void processSingleFile(File file, String outputDir, int blueIndex, int greenIndex, int redIndex,
                                   double targetMean, double minOverlapSize, double circleRadius) {
        ImagePlus imp = new Opener().openImage(file.getAbsolutePath());
        if (imp == null) {
            IJ.log("FSCell: unable to open " + file.getName());
            return;
        }

        ImagePlus[] channels = ChannelSplitter.split(imp);
        if (channels.length <= Math.max(Math.max(blueIndex, greenIndex), redIndex)) {
            IJ.log("FSCell: skipping " + file.getName() + " (not enough channels)");
            return;
        }

        ImagePlus blue = channels[blueIndex].duplicate();
        ImagePlus green = channels[greenIndex].duplicate();
        ImagePlus red = channels[redIndex].duplicate();

        normalizeMeanIntensity(red, targetMean);
        normalizeMeanIntensity(green, targetMean);

        ImagePlus merged = mergeChannels(blue, green, red);
        Overlay overlay = merged.getOverlay();
        if (overlay == null) {
            overlay = new Overlay();
        }

        addOverlapCircles(red, green, overlay, minOverlapSize, circleRadius);
        merged.setOverlay(overlay);

        String outputName = file.getName().replaceAll("(?i)\\.tiff?", "") + "_fs merged.tif";
        String outputPath = new File(outputDir, outputName).getAbsolutePath();
        IJ.saveAsTiff(merged, outputPath);
        IJ.log("FSCell: saved " + outputPath);
    }

    private void normalizeMeanIntensity(ImagePlus channel, double targetMean) {
        ImageProcessor processor = channel.getProcessor();
        ImageStatistics stats = ImageStatistics.getStatistics(processor, Measurements.MEAN, null);
        double currentMean = stats.mean;
        if (currentMean <= 0) {
            return;
        }
        double factor = targetMean / currentMean;
        processor.multiply(factor);
        processor.resetMinAndMax();
    }

    private ImagePlus mergeChannels(ImagePlus blue, ImagePlus green, ImagePlus red) {
        ImagePlus[] order = new ImagePlus[]{red, green, blue};
        return RGBStackMerge.mergeChannels(order, true);
    }

    private void addOverlapCircles(ImagePlus red, ImagePlus green, Overlay overlay,
                                   double minParticleSize, double circleRadius) {
        ByteProcessor redMask = thresholdToMask(red);
        ByteProcessor greenMask = thresholdToMask(green);
        ByteProcessor overlapMask = (ByteProcessor) redMask.duplicate();
        overlapMask.and(greenMask);

        ImagePlus overlapImage = new ImagePlus("overlap", overlapMask);
        ResultsTable rt = new ResultsTable();
        RoiManager manager = new RoiManager(false);
        int options = ParticleAnalyzer.CLEAR_WORKSHEET | ParticleAnalyzer.ADD_TO_MANAGER;
        int measurements = Measurements.CENTROID | Measurements.AREA;
        ParticleAnalyzer analyzer = new ParticleAnalyzer(options, measurements, rt, minParticleSize, Double.POSITIVE_INFINITY);
        analyzer.setRoiManager(manager);
        analyzer.analyze(overlapImage);

        Roi[] rois = manager.getRoisAsArray();
        for (int i = 0; i < rois.length; i++) {
            double x = rt.getValue("X", i);
            double y = rt.getValue("Y", i);
            Roi circle = new OvalRoi(x - circleRadius, y - circleRadius, circleRadius * 2, circleRadius * 2);
            circle.setStrokeColor(Color.YELLOW);
            circle.setStrokeWidth(1.5);
            overlay.add(circle);
        }
        manager.close();
    }

    private ByteProcessor thresholdToMask(ImagePlus channel) {
        ImageProcessor processor = channel.getProcessor().duplicate();
        if (!(processor instanceof ByteProcessor)) {
            processor = processor.convertToByte(true);
        }
        AutoThresholder.Method method = AutoThresholder.Method.OTSU;
        AutoThresholder thresholder = new AutoThresholder();
        int[] histogram = processor.getHistogram();
        int threshold = thresholder.getThreshold(method, histogram);
        ByteProcessor byteProcessor = (ByteProcessor) processor;
        byteProcessor.threshold(threshold);
        return byteProcessor;
    }

    private void runBlueNucleiCounter() {
        OpenDialog openDialog = new OpenDialog("Select image for blue nuclei counting", "");
        String directory = openDialog.getDirectory();
        String fileName = openDialog.getFileName();
        if (fileName == null) {
            IJ.showStatus("FSCell: nuclei counting cancelled");
            return;
        }
        ImagePlus source = new Opener().openImage(directory, fileName);
        if (source == null) {
            IJ.error("FSCell", "Unable to open selected image");
            return;
        }

        ImagePlus blueChannel = extractBlueChannel(source);
        if (blueChannel == null) {
            IJ.error("FSCell", "Unable to determine the blue channel. Please ensure the image contains at least one channel.");
            return;
        }

        BlueNucleiCounter counter = new BlueNucleiCounter(blueChannel);
        counter.show();
    }

    private ImagePlus extractBlueChannel(ImagePlus imp) {
        if (imp.getNChannels() == 1) {
            return imp.duplicate();
        }
        ImagePlus[] channels = ChannelSplitter.split(imp);
        int blueIndex = Math.min(2, channels.length - 1); // prefer 3rd channel, fallback to last
        return channels[blueIndex].duplicate();
    }

    private static class BlueNucleiCounter implements ij.gui.DialogListener {
        private final ImagePlus original;
        private ImagePlus display;
        private int tolerance;
        private NonBlockingGenericDialog dialog;

        BlueNucleiCounter(ImagePlus original) {
            this.original = original;
            this.tolerance = 10;
        }

        void show() {
            display = original.duplicate();
            display.setTitle(original.getShortTitle() + " - FSCell nuclei");
            display.show();
            display.setProperty("FSCell.minSize", 20.0);
            dialog = new NonBlockingGenericDialog("Blue nuclei tolerance");
            dialog.addSlider("Tolerance", 0, 50, tolerance);
            dialog.addNumericField("Minimum nucleus size (pixels)", 20, 0);
            dialog.addDialogListener(this);
            dialog.showDialog();
            if (!dialog.wasCanceled()) {
                updateOverlay();
            }
        }

        @Override
        public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
            gd.resetCounters();
            tolerance = (int) Math.round(gd.getNextNumber());
            double minSize = Math.max(1, gd.getNextNumber());
            display.setProperty("FSCell.minSize", minSize);
            updateOverlay();
            return true;
        }

        private void updateOverlay() {
            if (display == null || display.getWindow() == null) {
                return;
            }
            double minSize = getMinSize();
            ByteProcessor mask = createMask(tolerance);
            ImagePlus maskImage = new ImagePlus("mask", mask);
            ResultsTable rt = new ResultsTable();
            RoiManager manager = new RoiManager(false);
            int options = ParticleAnalyzer.CLEAR_WORKSHEET | ParticleAnalyzer.ADD_TO_MANAGER;
            int measurements = Measurements.CENTROID | Measurements.AREA;
            ParticleAnalyzer analyzer = new ParticleAnalyzer(options, measurements, rt, minSize, Double.POSITIVE_INFINITY);
            analyzer.setRoiManager(manager);
            analyzer.analyze(maskImage);

            Overlay overlay = new Overlay();
            Roi[] rois = manager.getRoisAsArray();
            for (Roi roi : rois) {
                Roi clone = (Roi) roi.clone();
                clone.setStrokeColor(new Color(0, 0, 255));
                clone.setStrokeWidth(1.3);
                overlay.add(clone);
            }
            display.setOverlay(overlay);
            display.updateAndDraw();

            int count = manager.getCount();
            IJ.showStatus("FSCell: nuclei count = " + count + " (tolerance=" + tolerance + ")");
            IJ.log("FSCell nuclei count: " + count + " (tolerance=" + tolerance + ", minSize=" + minSize + ")");
            manager.close();
        }

        private double getMinSize() {
            Object property = display.getProperty("FSCell.minSize");
            if (property instanceof Number) {
                return ((Number) property).doubleValue();
            }
            return 20.0;
        }

        private ByteProcessor createMask(int tolerance) {
            ImageProcessor processor = original.getProcessor().duplicate();
            if (!(processor instanceof ByteProcessor)) {
                processor = processor.convertToByte(true);
            }
            AutoThresholder.Method method = AutoThresholder.Method.OTSU;
            AutoThresholder thresholder = new AutoThresholder();
            int[] histogram = processor.getHistogram();
            int threshold = thresholder.getThreshold(method, histogram);
            threshold = Math.max(0, Math.min(255, threshold - tolerance));
            ByteProcessor byteProcessor = (ByteProcessor) processor;
            byteProcessor.threshold(threshold);
            return byteProcessor;
        }
    }
}
