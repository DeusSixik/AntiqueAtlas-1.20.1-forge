package hunternif.mc.impl.atlas.util;

import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.client.*;
import hunternif.mc.impl.atlas.client.gui.ExportUpdateListener;
import hunternif.mc.impl.atlas.core.ITileStorage;
import hunternif.mc.impl.atlas.core.WorldData;
import hunternif.mc.impl.atlas.marker.DimensionMarkersData;
import hunternif.mc.impl.atlas.marker.Marker;
import hunternif.mc.impl.atlas.marker.MarkersData;
import hunternif.mc.impl.atlas.registry.MarkerRenderInfo;
import hunternif.mc.impl.atlas.registry.MarkerType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ExportImageUtil {
    public static final int TILE_SIZE = 16;
    public static final int MARKER_SIZE = 32;
    public static boolean isExporting = false;

    private static Frame frame;
    private static final JFileChooser chooser = new JFileChooser();

    private static ExportUpdateListener getListener() {
        return ExportUpdateListener.INSTANCE;
    }

    static {
        chooser.setDialogTitle(I18n.get("gui.antiqueatlas.exportImage"));
        chooser.setSelectedFile(new File("Atlas.png"));
        chooser.setFileFilter(new FileFilter() {
            @Override
            public String getDescription() {
                return "PNG Image";
            }

            @Override
            public boolean accept(File file) {
                // Accept all files so they are visible
                return true;
            }
        });
    }

    /**
     * Beware that the background texture doesn't follow the Autotile format.
     */
    private static final int BG_TILE_SIZE = 22;

    /**
     * Opens a dialog and returns the file that was chosen, null if none or error.
     */
    public static File selectPngFileToSave(String atlasName) {
        getListener().setHeaderString("");
        getListener().setStatusString("gui.antiqueatlas.export.opening");
        getListener().setProgressMax(-1);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            Log.error(e, "Setting system Look&Feel for JFileChooser");
        }

        getListener().setStatusString("gui.antiqueatlas.export.selectFile");
        frame = new Frame();
        if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            frame.dispose();
            // Check file extension:
            if (file.getName().length() < 4 || // No extension
                    !file.getName().substring(file.getName().length() - 4).equalsIgnoreCase(".png")) {
                file = new File(file.getAbsolutePath() + ".png");
            }
            return file;
        }
        frame.dispose();
        return null;
    }

    /**
     * Renders the map into file as PNG image.
     */
    public static void exportPngImage(WorldData biomeData, DimensionMarkersData globalMarkers,
                                      DimensionMarkersData localMarkers, File file, boolean showMarkers, int step) {
        getListener().setHeaderString("gui.antiqueatlas.export.setup");
        ExportRenderArea renderArea = ExportRenderArea.from(biomeData.getScope(), step);
        // Prepare output image
        // Leave padding of one row of map tiles on each side
        int minX = renderArea.minBlockX;
        int minY = renderArea.minBlockY;
        int outWidth = renderArea.outWidth;
        int outHeight = renderArea.outHeight;
        Log.info("Image size: %dx%d", outWidth, outHeight);
        getListener().setStatusString("gui.antiqueatlas.export.makingbuffer", outWidth, outHeight);
        BufferedImage outImage = new BufferedImage(outWidth, outHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = outImage.createGraphics();

        // Draw background, double scale:
        int scale = 2;
        int bgTilesX = Math.round((float) outWidth / (float) BG_TILE_SIZE / (float) scale);
        int bgTilesY = Math.round((float) outHeight / (float) BG_TILE_SIZE / (float) scale);
        // Count background tiles too:

        // Preload all textures (they should be small enough)
        // Count loaded textures as update units too.
        getListener().setStatusString("gui.antiqueatlas.export.loadingtextures");
        getListener().setProgressMax(-1);
        BufferedImage bg = null;
        Map<ResourceLocation, BufferedImage> textureImageMap = new HashMap<>();
        try {
            InputStream is = Minecraft.getInstance().getResourceManager().open(Textures.EXPORTED_BG);
            bg = ImageIO.read(is);
            is.close();

            // Biome & Marker textures:
            java.util.List<ResourceLocation> allTextures = new ArrayList<>(64);
            allTextures.addAll(TileTextureMap.instance().getAllTextures());
            if (showMarkers) {
                for (MarkerType type : MarkerType.REGISTRY) {
                    allTextures.addAll(Arrays.asList(type.getAllIcons()));
//					allTextures.add(type.getIcon());
                }
            }
            for (ResourceLocation texture : allTextures) {
                try {
                    is = Minecraft.getInstance().getResourceManager().open(texture);
                    BufferedImage tileImage = ImageIO.read(is);
                    is.close();
                    textureImageMap.put(texture, tileImage);
                } catch (FileNotFoundException e) {
                    // This can happen, for example, when you remove a mod that has added custom textures
                    Log.warn("Texture %s not found!", texture.toString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        getListener().setHeaderString("gui.antiqueatlas.export.rendering");
        drawMapToGraphics(
                graphics,
                bgTilesX, bgTilesY, outWidth, outHeight,
                biomeData, textureImageMap,
                globalMarkers, localMarkers,
                showMarkers, minX, minY,
                renderArea.tileScope, renderArea.step,
                scale, bg);

        try {
            getListener().setHeaderString("");
            getListener().setStatusString("gui.antiqueatlas.export.writing");
            ImageIO.write(outImage, "PNG", file);
            Log.info("Done writing image");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Renders the map into file as PNG image stripe by stripe in order to not have a OutOfMemoryError.
     */
    public static void exportPngImageTooLarge(final WorldData biomeData, final DimensionMarkersData globalMarkers,
                                              final DimensionMarkersData localMarkers, File file, final boolean showMarkers, int step) {
        getListener().setHeaderString("");
        final ExportRenderArea renderArea = ExportRenderArea.from(biomeData.getScope(), step);
        // Prepare output image
        // Leave padding of one row of map tiles on each side
        final int minX = renderArea.minBlockX;
        final int minY = renderArea.minBlockY;
        final int outWidth = renderArea.outWidth;
        final int outHeight = renderArea.outHeight;
        Log.info("Image size: %dx%d", outWidth, outHeight);

        // Draw background, double scale:
        final int scale = 2;
        final int bgTilesX = Math.round((float) outWidth / (float) BG_TILE_SIZE / (float) scale);
        final int bgTilesY = Math.round((float) outHeight / (float) BG_TILE_SIZE / (float) scale);

        // Preload all textures (they should be small enough)
        // Count loaded textures as update units too.
        getListener().setStatusString("gui.antiqueatlas.export.loadingtextures");
        getListener().setProgressMax(-1);
        BufferedImage bg = null;
        final Map<ResourceLocation, BufferedImage> textureImageMap = new HashMap<>();
        try {
            InputStream is = Minecraft.getInstance().getResourceManager().open(Textures.EXPORTED_BG);
            bg = ImageIO.read(is);
            is.close();

            // Biome & Marker textures:
            java.util.List<ResourceLocation> allTextures = new ArrayList<>(64);
            allTextures.addAll(TileTextureMap.instance().getAllTextures());
            if (showMarkers) {
                for (MarkerType type : MarkerType.REGISTRY) {
                    allTextures.addAll(Arrays.asList(type.getAllIcons()));
//					allTextures.add(type.getIcon());
                }
            }
            for (ResourceLocation texture : allTextures) {
                try {
                    is = Minecraft.getInstance().getResourceManager().open(texture);
                    BufferedImage tileImage = ImageIO.read(is);
                    is.close();
                    textureImageMap.put(texture, tileImage);
                } catch (FileNotFoundException e) {
                    // This can happen, for example, when you remove a mod that has added custom textures
                    Log.warn("Texture %s not found!", texture.toString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.gc();

        long availableMem = getAvailableMemory();
        long usableMem = (long) (availableMem * 0.8); // leave some breathing room
        int pixelSize = Integer.SIZE / 8;

        int sliceHeight = TILE_SIZE;

        for (int i = bgTilesY; i > 0; i--) {
            long usedMem = ((long) (i * TILE_SIZE) * outWidth * pixelSize);
            if (usedMem <= usableMem) {
                sliceHeight = i * TILE_SIZE;
                break;
            } else {
                Log.info("%d tiles tall is too big, %d > %d", i, usedMem, usableMem);
            }
        }
        final int sliceHeight_ = sliceHeight;
        final int slices = (int) Math.ceil((float) outHeight / (float) sliceHeight);
        final BufferedImage bg_ = bg;

        final BufferedImage scanBuffer = new BufferedImage(outWidth, sliceHeight, BufferedImage.TYPE_INT_ARGB);

        getListener().setProgressMax(slices);
        RenderedImage outImage = new RenderedImageScanned(outWidth, outHeight, scanBuffer, graphics -> {
            int slice = (int) Math.floor(-graphics.getTransform().getTranslateY() / sliceHeight_);
            getListener().setProgress(slice);
            getListener().setHeaderString("gui.antiqueatlas.export.renderstripe", slice + 1, slices);
            drawMapToGraphics(
                    graphics,
                    bgTilesX, bgTilesY, outWidth, outHeight,
                    biomeData, textureImageMap,
                    globalMarkers, localMarkers,
                    showMarkers, minX, minY,
                    renderArea.tileScope, renderArea.step,
                    scale, bg_);
            getListener().setStatusString("gui.antiqueatlas.export.writestripe");
            getListener().setProgressMax(sliceHeight_ * (slice + 1) > outHeight ? outHeight - (sliceHeight_ * slice) : sliceHeight_);
        }, value -> getListener().setProgress(value));

        try {
            getListener().setHeaderString("gui.antiqueatlas.export.renderstripe", 1, slices);
            ImageIO.write(outImage, "PNG", file);
            Log.info("Done writing image");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static long getAvailableMemory() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory(); // current heap allocated to the VM process
        long freeMemory = runtime.freeMemory(); // out of the current heap, how much is free
        long maxMemory = runtime.maxMemory(); // Max heap VM can use e.g. Xmx setting
        long usedMemory = totalMemory - freeMemory; // how much of the current heap the VM is using

        return maxMemory - usedMemory; // available memory i.e. Maximum heap size minus the current amount used
    }

    private static TileRenderIterator createTileIterator(WorldData biomeData, Rect scope, int step) {
        ITileStorage storage = AntiqueAtlas.lodTileAggregationService.createStorage(
                biomeData,
                scope,
                step,
                biomeData.world.location()
        );
        TileRenderIterator iter = new TileRenderIterator(storage);
        iter.setScope(scope);
        iter.setStep(step);
        return iter;
    }

    private static int alignChunkToStep(int chunk, int step) {
        return Math.floorDiv(chunk, step) * step;
    }

    private static int getTileCount(int minChunk, int maxChunk, int step) {
        return Math.floorDiv(maxChunk - minChunk, step) + 1;
    }

    private static final class ExportRenderArea {
        private final Rect tileScope;
        private final int step;
        private final int minBlockX;
        private final int minBlockY;
        private final int outWidth;
        private final int outHeight;

        private ExportRenderArea(Rect tileScope, int step, int minBlockX, int minBlockY, int outWidth, int outHeight) {
            this.tileScope = tileScope;
            this.step = step;
            this.minBlockX = minBlockX;
            this.minBlockY = minBlockY;
            this.outWidth = outWidth;
            this.outHeight = outHeight;
        }

        private static ExportRenderArea from(Rect sourceScope, int requestedStep) {
            int step = Math.max(1, requestedStep);
            int minChunkX = alignChunkToStep(sourceScope.minX, step);
            int minChunkY = alignChunkToStep(sourceScope.minY, step);
            int maxChunkX = alignChunkToStep(sourceScope.maxX, step);
            int maxChunkY = alignChunkToStep(sourceScope.maxY, step);
            Rect tileScope = new Rect(minChunkX, minChunkY, maxChunkX, maxChunkY);
            int outWidth = (getTileCount(minChunkX, maxChunkX, step) + 2) * TILE_SIZE;
            int outHeight = (getTileCount(minChunkY, maxChunkY, step) + 2) * TILE_SIZE;
            int minBlockX = (minChunkX - step) * TILE_SIZE;
            int minBlockY = (minChunkY - step) * TILE_SIZE;
            return new ExportRenderArea(tileScope, step, minBlockX, minBlockY, outWidth, outHeight);
        }
    }

    private static void drawMapToGraphics(Graphics2D graphics,
                                          int bgTilesX, int bgTilesY, int outWidth, int outHeight,
                                          WorldData biomeData, Map<ResourceLocation, BufferedImage> textureImageMap,
                                          DimensionMarkersData globalMarkers, DimensionMarkersData localMarkers,
                                          boolean showMarkers, int minX, int minY, Rect tileScope, int step,
                                          int scale, BufferedImage bg) {
        getListener().setStatusString("gui.antiqueatlas.export.rendering.background");
        getListener().setProgressMax(bgTilesX * bgTilesY);
        //================ Draw map background ================
        // Top left corner:
        graphics.drawImage(bg, 0, 0, BG_TILE_SIZE * scale, BG_TILE_SIZE * scale,
                0, 0, BG_TILE_SIZE, BG_TILE_SIZE, null);
        getListener().addProgress(1);
        // Topmost row:
        for (int x = 1; x < bgTilesX; x++) {
            graphics.drawImage(bg, x * BG_TILE_SIZE * scale, 0, (x + 1) * BG_TILE_SIZE * scale, BG_TILE_SIZE * scale,
                    BG_TILE_SIZE, 0, BG_TILE_SIZE * 2, BG_TILE_SIZE, null);
            getListener().addProgress(1);
        }
        // Leftmost column:
        for (int y = 1; y < bgTilesY; y++) {
            graphics.drawImage(bg, 0, y * BG_TILE_SIZE * scale, BG_TILE_SIZE * scale, (y + 1) * BG_TILE_SIZE * scale,
                    0, BG_TILE_SIZE, BG_TILE_SIZE, BG_TILE_SIZE * 2, null);
            getListener().addProgress(1);
        }
        // Middle:
        for (int x = 1; x < bgTilesX; x++) {
            for (int y = 1; y < bgTilesY; y++) {
                graphics.drawImage(bg,
                        x * BG_TILE_SIZE * scale, y * BG_TILE_SIZE * scale,
                        (x + 1) * BG_TILE_SIZE * scale, (y + 1) * BG_TILE_SIZE * scale,
                        BG_TILE_SIZE, BG_TILE_SIZE, BG_TILE_SIZE * 2, BG_TILE_SIZE * 2, null);
                getListener().addProgress(1);
            }
        }
        // Top right corner:
        graphics.drawImage(bg, outWidth - BG_TILE_SIZE * scale, 0,
                outWidth, BG_TILE_SIZE * scale,
                BG_TILE_SIZE * 2, 0, BG_TILE_SIZE * 3, BG_TILE_SIZE, null);
        getListener().addProgress(1);
        // Rightmost column:
        for (int y = 1; y < bgTilesY; y++) {
            graphics.drawImage(bg,
                    outWidth - BG_TILE_SIZE * scale, y * BG_TILE_SIZE * scale,
                    outWidth, (y + 1) * BG_TILE_SIZE * scale,
                    BG_TILE_SIZE * 2, BG_TILE_SIZE, BG_TILE_SIZE * 3, BG_TILE_SIZE * 2, null);
            getListener().addProgress(1);
        }
        // Bottom left corner:
        graphics.drawImage(bg, 0, outHeight - BG_TILE_SIZE * scale,
                BG_TILE_SIZE * scale, outHeight,
                0, BG_TILE_SIZE * 2, BG_TILE_SIZE, BG_TILE_SIZE * 3, null);
        getListener().addProgress(1);
        // Bottommost row:
        for (int x = 1; x < bgTilesX; x++) {
            graphics.drawImage(bg, x * BG_TILE_SIZE * scale, outHeight - BG_TILE_SIZE * scale,
                    (x + 1) * BG_TILE_SIZE * scale, outHeight,
                    BG_TILE_SIZE, BG_TILE_SIZE * 2, BG_TILE_SIZE * 2, BG_TILE_SIZE * 3, null);
            getListener().addProgress(1);
        }
        // Bottom right corner:
        graphics.drawImage(bg, outWidth - BG_TILE_SIZE * scale, outHeight - BG_TILE_SIZE * scale,
                outWidth, outHeight, BG_TILE_SIZE * 2, BG_TILE_SIZE * 2, BG_TILE_SIZE * 3, BG_TILE_SIZE * 3, null);
        getListener().addProgress(1);

        //============= Draw actual map tiles ==============
        getListener().setStatusString("gui.antiqueatlas.export.rendering.map");
        int tileColumns = getTileCount(tileScope.minX, tileScope.maxX, step);
        int tileRows = getTileCount(tileScope.minY, tileScope.maxY, step);
        getListener().setProgressMax(tileColumns * tileRows);

        TileRenderIterator iter = createTileIterator(biomeData, tileScope, step);
        while (iter.hasNext()) {
            SubTileQuartet subtiles = iter.next();
            for (SubTile subtile : subtiles) {
                if (subtile == null || subtile.tile == null) continue;

                // Load tile texture
                ResourceLocation texture = TileTextureMap.instance().getTexture(subtile).getTexture();
                BufferedImage tileImage = textureImageMap.get(texture);
                if (tileImage == null) continue;

                graphics.drawImage(tileImage,

                        TILE_SIZE + subtile.x * TILE_SIZE / 2,
                        TILE_SIZE + subtile.y * TILE_SIZE / 2,

                        TILE_SIZE + (subtile.x + 1) * TILE_SIZE / 2,
                        TILE_SIZE + (subtile.y + 1) * TILE_SIZE / 2,

                        subtile.getTextureU() * TILE_SIZE / 2,
                        subtile.getTextureV() * TILE_SIZE / 2,

                        (subtile.getTextureU() + 1) * TILE_SIZE / 2,
                        (subtile.getTextureV() + 1) * TILE_SIZE / 2,

                        null);
            }
            getListener().addProgress(1);
        }

        //============== Draw markers ================
        // Draw local markers on top of global markers
        getListener().setStatusString("gui.antiqueatlas.export.rendering.markers");
        getListener().setProgressMax(-1);

        java.util.List<Marker> markers = new ArrayList<>();
        for (int x = biomeData.getScope().minX / MarkersData.CHUNK_STEP;
             x <= biomeData.getScope().maxX / MarkersData.CHUNK_STEP; x++) {
            for (int z = biomeData.getScope().minY / MarkersData.CHUNK_STEP;
                 z <= biomeData.getScope().maxY / MarkersData.CHUNK_STEP; z++) {

                markers.clear();
                java.util.List<Marker> globalMarkersAt = globalMarkers.getMarkersAtChunk(x, z);
                if (globalMarkersAt != null) {
                    markers.addAll(globalMarkers.getMarkersAtChunk(x, z));
                }
                if (localMarkers != null) {
                	java.util.List<Marker> localMarkersAt = localMarkers.getMarkersAtChunk(x, z);
                    if (localMarkersAt != null) {
                        markers.addAll(localMarkersAt);
                    }
                }

                for (Marker marker : markers) {
                    MarkerType type = MarkerType.REGISTRY.get(marker.getType());
                    if (type == null) {
                        Log.warn("Could not find marker data for type: %s\n", marker.getType());
                        continue;
                    }

                    if (!marker.isVisibleAhead() &&
                            !biomeData.hasTileAt(marker.getChunkX(), marker.getChunkZ())) {
                        continue;
                    }

                    if (type.shouldHide(!showMarkers, 0)) {
                        continue;
                    }

                    type.calculateMip(1, 1, 1);
                    MarkerRenderInfo info = type.getRenderInfo(1, 1, 1);
                    type.resetMip();

                    // Load marker texture
                    ResourceLocation texture = info.tex.getTexture();
                    BufferedImage markerImage = textureImageMap.get(texture);
                    if (markerImage == null)
                        continue;

                    int markerX = (int) Math.round((marker.getX() - minX) / (double) step);
                    int markerY = (int) Math.round((marker.getZ() - minY) / (double) step);

                    graphics.drawImage(
                            markerImage,
                            (int) (markerX + info.x), (int) (markerY + info.y),
                            info.tex.width(), info.tex.height(), null);
                }
            }
        }
    }
}
