# FSCell

FSCell (Fluorescence SmartCell) is an ImageJ/Fiji plugin designed to automate repetitive multi-channel fluorescence microscopy workflows. It balances channels, detects red/green overlaps, and assists with nuclei counting across batches of `.tif/.tiff` images that contain four color channels (blue, green, red, and an optional marker).

> Looking for the Chinese documentation? See [《读我！》](读我！.md).

## Features

- **Batch channel merge & overlap export**
  - Select a folder of multi-channel TIFF images and an output folder.
  - Automatically normalizes the red and green channels to a configurable mean intensity so their exposure appears balanced.
  - Merges the blue, green, and red channels into a composite image and exports a processed TIFF per input file.
  - Detects overlapping red/green regions, draws circles around each overlap, and includes the overlay in the exported image.
- **Interactive blue nuclei counter**
  - Counts blue-channel nuclei using an automatic threshold (Otsu) with an adjustable tolerance slider.
  - Offers a minimum area control to avoid small debris, with live updates to the overlay and count.
  - Intended for quick quality control after batch processing; every adjustment immediately refreshes the overlay.

## Building

The project uses Maven and targets Java 8. To build the plugin JAR:

```bash
mvn clean package
```

The compiled artifact will appear in `target/fs-cell-0.1.0-SNAPSHOT.jar`.

## Installation

1. Build the project or download the released JAR.
2. Copy the JAR into your ImageJ/Fiji `plugins` directory (for Fiji this is typically `{Fiji.app}/plugins`).
3. Copy `src/main/resources/plugins.config` into the same directory if you are installing manually. When bundling the JAR the configuration file is already included.
4. Restart ImageJ/Fiji. You will find the plugin under **Plugins ▸ FSCell ▸ Batch Merge and Count**.

## Usage

### Batch merge & overlap export

1. Launch the plugin and choose **Batch merge & overlap export**.
2. Select the input directory containing `.tif/.tiff` files with stacked channels. Files must contain at least the blue, green, and red channels (default indices 1–3).
3. Choose an output directory for processed images.
4. Configure the channel indices, target mean intensity, minimum overlap size, and circle radius.
5. FSCell will generate composite images with balanced red/green exposures and highlight red/green overlaps using yellow circles.

### Interactive blue nuclei counter

1. Launch the plugin and choose **Interactive blue nuclei counter**.
2. Select a TIFF image (multi-channel or single-channel). FSCell extracts the blue channel automatically (prefers channel 3).
3. A preview window appears alongside a tolerance slider and minimum size field. Adjust as needed to ensure all nuclei are detected.
4. The live overlay shows detected nuclei outlines and the status bar reports the count.

## Contributing

Contributions are welcome! Feel free to open issues or submit pull requests with improvements, bug fixes, or feature suggestions.
