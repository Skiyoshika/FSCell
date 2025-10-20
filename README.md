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

## Building (optional)

Because this repository is mirrored into environments that reject binary blobs, the prebuilt Fiji drop-in archive is not checked into version control. If you modify the source or want to produce fresh artifacts yourself, run:

```bash
mvn clean package
```

This creates `target/FSCell.jar` and assembles `target/FSCell-fiji.zip`.

For convenience there is also a helper script that copies the assembled ZIP into the `distribution/` directory:

```bash
./scripts/create-dropin-zip.sh
```

> **Note:** The generated ZIP is ignored by git, so the script must be re-run whenever you need a fresh drop-in package.

## Installation

### Quick install (recommended)

1. Download the ready-to-use archive from the project release page, or generate it locally with `./scripts/create-dropin-zip.sh` (the script will place `FSCell-fiji.zip` in `distribution/`).
2. Extract the archive directly into your ImageJ/Fiji `plugins` directory (e.g. `{Fiji.app}/plugins`). The archive expands to `plugins/FSCell/` containing the JAR, plugin registration file, and a short install guide.
3. Restart ImageJ/Fiji. You will find the plugin under **Plugins ▸ FSCell ▸ Batch Merge and Count**.

### Manual install (advanced)

1. Build the project with Maven (`mvn clean package`) or compile `src/main/java` manually to produce `FSCell.jar`.
2. Copy `FSCell.jar` into your ImageJ/Fiji `plugins` directory.
3. Ensure `plugins.config` is located alongside the JAR. The ready-made ZIP already contains a copy at `plugins/FSCell/plugins.config`.
4. Restart ImageJ/Fiji.

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
