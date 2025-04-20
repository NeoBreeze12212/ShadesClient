package org.neo.shadesclient.modules.configs;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.neo.shadesclient.client.ShadesClient;
import org.neo.shadesclient.modules.WaypointsModule;
import org.neo.shadesclient.modules.WaypointsModule.Waypoint;
import org.neo.shadesclient.utils.ColorPickerWidget;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class WaypointConfigScreen extends Screen {
    private final WaypointsModule module;
    private final Screen parentScreen;

    private static final int BACKGROUND_COLOR = 0x90000000;
    private static final int HEADER_COLOR = 0xFF1A1A1A;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int BUTTON_COLOR = 0xFF303030;
    private static final int BUTTON_HOVER_COLOR = 0xFF404040;
    private static final int LIST_ENTRY_COLOR = 0xFF262626;
    private static final int LIST_ENTRY_HOVER_COLOR = 0xFF383838;
    private static final int LIST_ENTRY_SELECTED_COLOR = 0xFF3050CF;

    // UI Components
    private ButtonWidget backButton;
    private ButtonWidget addWaypointButton;
    private ButtonWidget editWaypointButton;
    private ButtonWidget deleteWaypointButton;

    // Settings components
    private ButtonWidget toggleShowDistanceButton;
    private ButtonWidget toggleShowCoordinatesButton;
    private ButtonWidget toggleRenderBeaconsButton;
    private ButtonWidget toggleRenderLabelsButton;
    private SliderWidget maxRenderDistanceSlider;
    private SliderWidget beaconHeightSlider;
    private SliderWidget labelScaleSlider;

    // Waypoint list and selection
    private List<WaypointListEntry> waypointEntries = new ArrayList<>();
    private WaypointListEntry selectedEntry = null;
    private int listScrollOffset = 0;
    private final int entriesPerPage = 8;

    // Add/Edit waypoint components
    private boolean showAddWaypointPanel = false;
    private boolean editingExistingWaypoint = false;
    private UUID editingWaypointId = null;
    private TextFieldWidget nameField;
    private ColorPickerWidget colorPicker;

    public WaypointConfigScreen(WaypointsModule module) {
        super(Text.literal("Waypoints Configuration"));
        this.module = module;
        this.parentScreen = MinecraftClient.getInstance().currentScreen;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int buttonWidth = 150;

        // Back button
        backButton = ButtonWidget.builder(Text.literal("Back"), button -> {
            MinecraftClient.getInstance().setScreen(parentScreen);
        }).dimensions(10, 10, 60, 20).build();
        addDrawableChild(backButton);

        // Add setting controls
        int settingsX = centerX - 200;
        int settingsY = 60;
        int settingsWidth = 190;

        // Show distance toggle
        toggleShowDistanceButton = ButtonWidget.builder(
                Text.literal("Show Distance: " + (module.isShowDistance() ? "ON" : "OFF")),
                button -> {
                    module.setShowDistance(!module.isShowDistance());
                    button.setMessage(Text.literal("Show Distance: " + (module.isShowDistance() ? "ON" : "OFF")));
                }).dimensions(settingsX, settingsY, settingsWidth, 20).build();
        addDrawableChild(toggleShowDistanceButton);
        settingsY += 30;

        // Show coordinates toggle
        toggleShowCoordinatesButton = ButtonWidget.builder(
                Text.literal("Show Coordinates: " + (module.isShowCoordinates() ? "ON" : "OFF")),
                button -> {
                    module.setShowCoordinates(!module.isShowCoordinates());
                    button.setMessage(Text.literal("Show Coordinates: " + (module.isShowCoordinates() ? "ON" : "OFF")));
                }).dimensions(settingsX, settingsY, settingsWidth, 20).build();
        addDrawableChild(toggleShowCoordinatesButton);
        settingsY += 30;

        // Render beacons toggle
        toggleRenderBeaconsButton = ButtonWidget.builder(
                Text.literal("Render Beacons: " + (module.isRenderBeacons() ? "ON" : "OFF")),
                button -> {
                    module.setRenderBeacons(!module.isRenderBeacons());
                    button.setMessage(Text.literal("Render Beacons: " + (module.isRenderBeacons() ? "ON" : "OFF")));
                }).dimensions(settingsX, settingsY, settingsWidth, 20).build();
        addDrawableChild(toggleRenderBeaconsButton);
        settingsY += 30;

        // Render labels toggle
        toggleRenderLabelsButton = ButtonWidget.builder(
                Text.literal("Render Labels: " + (module.isRenderLabels() ? "ON" : "OFF")),
                button -> {
                    module.setRenderLabels(!module.isRenderLabels());
                    button.setMessage(Text.literal("Render Labels: " + (module.isRenderLabels() ? "ON" : "OFF")));
                }).dimensions(settingsX, settingsY, settingsWidth, 20).build();
        addDrawableChild(toggleRenderLabelsButton);
        settingsY += 30;

        // Max render distance slider
        maxRenderDistanceSlider = new RangeSliderWidget(
                settingsX, settingsY, settingsWidth, 20,
                Text.literal("Max Render Distance: " + module.getMaxRenderDistance()),
                module.getMaxRenderDistance() / 512.0f, // slider value 0-1
                32, 512, // min-max range
                value -> {
                    // Fix for issue #1: explicitly cast to int
                    module.setMaxRenderDistance((int)value);
                    maxRenderDistanceSlider.setMessage(Text.literal("Max Render Distance: " + (int)value));
                }
        );
        addDrawableChild(maxRenderDistanceSlider);
        settingsY += 30;

        // Beacon height slider
        beaconHeightSlider = new RangeSliderWidget(
                settingsX, settingsY, settingsWidth, 20,
                Text.literal("Beacon Height: " + String.format("%.1f", module.getBeaconHeight())),
                module.getBeaconHeight() / 200.0f, // slider value 0-1
                10, 200, // min-max range
                value -> {
                    float heightValue = value.floatValue(); // explicitly convert to float
                    module.setBeaconHeight(heightValue);
                    beaconHeightSlider.setMessage(Text.literal("Beacon Height: " + String.format("%.1f", heightValue)));
                }
        );
        addDrawableChild(beaconHeightSlider);
        settingsY += 30;

        // Label scale slider
        labelScaleSlider = new RangeSliderWidget(
                settingsX, settingsY, settingsWidth, 20,
                Text.literal("Label Scale: " + String.format("%.1f", module.getLabelScale())),
                (module.getLabelScale() - 0.5f) / 2.5f, // slider value 0-1 mapping to 0.5-3.0
                0.5f, 3.0f, // min-max range
                value -> {
                    float scaleValue = value.floatValue(); // explicitly convert to float
                    module.setLabelScale(scaleValue);
                    labelScaleSlider.setMessage(Text.literal("Label Scale: " + String.format("%.1f", scaleValue)));
                }
        );
        addDrawableChild(labelScaleSlider);

        // Waypoint list management
        int waypointListX = centerX + 10;
        int waypointListY = 60;
        int listWidth = 190;

        // Waypoint list buttons
        addWaypointButton = ButtonWidget.builder(Text.literal("Add Waypoint"), button -> {
            showAddWaypointPanel = true;
            editingExistingWaypoint = false;
            if (nameField != null) {
                nameField.setText("");
            }
        }).dimensions(waypointListX, height - 60, 95, 20).build();
        addDrawableChild(addWaypointButton);

        editWaypointButton = ButtonWidget.builder(Text.literal("Edit"), button -> {
            if (selectedEntry != null) {
                showAddWaypointPanel = true;
                editingExistingWaypoint = true;
                editingWaypointId = selectedEntry.waypoint.getId();
                if (nameField != null) {
                    nameField.setText(selectedEntry.waypoint.getName());
                }
                if (colorPicker != null) {
                    colorPicker.setSelectedColor(selectedEntry.waypoint.getColor());
                }
            }
        }).dimensions(waypointListX + 100, height - 60, 90, 20).build();
        addDrawableChild(editWaypointButton);

        deleteWaypointButton = ButtonWidget.builder(Text.literal("Delete"), button -> {
            if (selectedEntry != null) {
                module.removeWaypoint(selectedEntry.waypoint.getId());
                refreshWaypointList();
                selectedEntry = null;
                updateButtonStates();
            }
        }).dimensions(waypointListX, height - 30, 190, 20).build();
        addDrawableChild(deleteWaypointButton);

        // Scroll buttons
        addDrawableChild(ButtonWidget.builder(Text.literal("▲"), button -> {
            if (listScrollOffset > 0) {
                listScrollOffset--;
            }
        }).dimensions(waypointListX + listWidth - 20, waypointListY, 20, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("▼"), button -> {
            if (listScrollOffset < Math.max(0, waypointEntries.size() - entriesPerPage)) {
                listScrollOffset++;
            }
        }).dimensions(waypointListX + listWidth - 20, waypointListY + 240, 20, 20).build());

        // Add/Edit waypoint panel components
        if (showAddWaypointPanel) {
            setupAddWaypointPanel();
        }

        // Load waypoint list
        refreshWaypointList();
        updateButtonStates();
    }

    private void setupAddWaypointPanel() {
        int panelWidth = 300;
        int panelHeight = 180;
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;

        // Name field
        nameField = new TextFieldWidget(
                textRenderer,
                panelX + 80, panelY + 30,
                200, 20,
                Text.literal("Waypoint Name")
        );
        nameField.setMaxLength(32);
        nameField.setVisible(true);
        nameField.setEditable(true);

        if (editingExistingWaypoint && selectedEntry != null) {
            nameField.setText(selectedEntry.waypoint.getName());
        }

        addDrawableChild(nameField);

        // Color picker
        int initialColor = editingExistingWaypoint && selectedEntry != null
                ? selectedEntry.waypoint.getColor()
                : 0xFF0000; // Default red

        colorPicker = new ColorPickerWidget(
                panelX + 80, panelY + 60,
                200, 20,
                Text.literal("Color"),
                initialColor
        );
        addDrawableChild(colorPicker);

        // Cancel button - Fix for issue #2: Define cancelButton before referencing it
        ButtonWidget cancelButton = ButtonWidget.builder(
                Text.literal("Cancel"),
                button -> {
                    showAddWaypointPanel = false;
                    editingExistingWaypoint = false;

                    // Remove panel components
                    remove(nameField);
                    remove(colorPicker);
                    // We can't reference saveButton here yet, so we'll handle it differently
                    children().stream()
                            .filter(child -> child instanceof ButtonWidget &&
                                    ((ButtonWidget)child).getMessage().getString().equals(editingExistingWaypoint ? "Update" : "Create"))
                            .forEach(this::remove);
                    remove(button);
                }
        ).dimensions(panelX + 75, panelY + 130, 70, 20).build();

        // Save button
        ButtonWidget saveButton = ButtonWidget.builder(
                Text.literal(editingExistingWaypoint ? "Update" : "Create"),
                button -> {
                    String name = nameField.getText().trim();
                    if (name.isEmpty()) {
                        // Show error or flash the field
                        return;
                    }

                    int color = colorPicker.getSelectedColor();

                    if (editingExistingWaypoint) {
                        // Update existing waypoint
                        for (WaypointListEntry entry : waypointEntries) {
                            if (entry.waypoint.getId().equals(editingWaypointId)) {
                                entry.waypoint.setName(name);
                                entry.waypoint.setColor(color);
                                break;
                            }
                        }
                    } else {
                        // Create new waypoint at player position
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client.player != null) {
                            BlockPos pos = client.player.getBlockPos();
                            String dimension = client.world.getRegistryKey().getValue().toString();
                            String worldId = getCurrentWorldIdentifier();
                            module.addWaypoint(name, pos, dimension, color, worldId);
                        }
                    }

                    // Close panel and refresh list
                    showAddWaypointPanel = false;
                    refreshWaypointList();

                    // Remove panel components
                    remove(nameField);
                    remove(colorPicker);
                    remove(button);
                    remove(cancelButton);
                }
        ).dimensions(panelX + 155, panelY + 130, 70, 20).build();

        addDrawableChild(saveButton);
        addDrawableChild(cancelButton);
    }

    private String getCurrentWorldIdentifier() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getServer() != null) {
            // Single player world
            try {
                // Try to get the world name directly
                return "singleplayer:" + client.getServer().getSaveProperties().getLevelName();
            } catch (Exception e) {
                // Fallback method using file path
                return "singleplayer:" + new File(String.valueOf(client.getServer().getRunDirectory()), "saves").listFiles()[0].getName();
            }
        } else if (client.getCurrentServerEntry() != null) {
            // Multiplayer server
            return "server:" + client.getCurrentServerEntry().address;
        }
        return "unknown";
    }

    private void refreshWaypointList() {
        waypointEntries.clear();

        // Get waypoints for current world
        String worldId = getCurrentWorldIdentifier();
        List<Waypoint> waypoints = module.getWaypointsInWorld(worldId);

        for (Waypoint waypoint : waypoints) {
            waypointEntries.add(new WaypointListEntry(waypoint));
        }

        // Adjust scroll offset if needed
        if (listScrollOffset > Math.max(0, waypointEntries.size() - entriesPerPage)) {
            listScrollOffset = Math.max(0, waypointEntries.size() - entriesPerPage);
        }
    }

    private void updateButtonStates() {
        boolean hasSelection = selectedEntry != null;
        editWaypointButton.active = hasSelection;
        deleteWaypointButton.active = hasSelection;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Fill the background
        renderBackground(context);

        super.render(context, mouseX, mouseY, delta);

        // Draw title
        context.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, 15, TEXT_COLOR);

        // Draw settings section header
        context.drawTextWithShadow(textRenderer, Text.literal("Settings"), width / 2 - 200, 40, TEXT_COLOR);

        // Draw waypoint list section header
        context.drawTextWithShadow(textRenderer, Text.literal("Waypoints"), width / 2 + 10, 40, TEXT_COLOR);

        // Draw waypoint list
        int waypointListX = width / 2 + 10;
        int waypointListY = 60;
        int listWidth = 190;
        int entryHeight = 30;

        // List background
        context.fill(waypointListX, waypointListY, waypointListX + listWidth - 20, waypointListY + 240, 0x80000000);

        // Draw visible entries
        int visibleCount = Math.min(entriesPerPage, waypointEntries.size() - listScrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            int index = i + listScrollOffset;
            if (index >= 0 && index < waypointEntries.size()) {
                WaypointListEntry entry = waypointEntries.get(index);
                int entryY = waypointListY + i * entryHeight;

                // Background
                int bgColor = entry == selectedEntry ? LIST_ENTRY_SELECTED_COLOR :
                        isMouseOverEntry(mouseX, mouseY, waypointListX, entryY, listWidth - 20, entryHeight) ?
                                LIST_ENTRY_HOVER_COLOR : LIST_ENTRY_COLOR;

                context.fill(waypointListX, entryY, waypointListX + listWidth - 20, entryY + entryHeight, bgColor);

                // Draw entry content
                BlockPos pos = entry.waypoint.getPosition();
                String coords = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();

                // Color indicator
                context.fill(waypointListX + 5, entryY + 5, waypointListX + 15, entryY + 15, entry.waypoint.getColor() | 0xFF000000);
                context.fill(waypointListX + 5, entryY + 5, waypointListX + 15, entryY + 15, 0x20000000);

                // Name and coordinates
                context.drawTextWithShadow(textRenderer, Text.literal(entry.waypoint.getName()), waypointListX + 20, entryY + 5, TEXT_COLOR);
                context.drawTextWithShadow(textRenderer, Text.literal(coords), waypointListX + 20, entryY + 17, 0xAAAAAA);

                // Visibility toggle
                String visibilitySymbol = entry.waypoint.isVisible() ? "👁" : "⊘";
                int visibilityX = waypointListX + listWidth - 35;
                int visibilityColor = entry.waypoint.isVisible() ? 0xFFFFFFFF : 0xFFAAAAAA;

                context.drawTextWithShadow(textRenderer, Text.literal(visibilitySymbol), visibilityX, entryY + 10, visibilityColor);
            }
        }

        // Draw add/edit waypoint panel
        if (showAddWaypointPanel) {
            int panelWidth = 300;
            int panelHeight = 180;
            int panelX = (width - panelWidth) / 2;
            int panelY = (height - panelHeight) / 2;

            // Panel background
            context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE0000000);
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(editingExistingWaypoint ? "Edit Waypoint" : "Add Waypoint"),
                    width / 2, panelY + 10, TEXT_COLOR);

            // Labels
            context.drawTextWithShadow(textRenderer, Text.literal("Name:"), panelX + 20, panelY + 35, TEXT_COLOR);
            context.drawTextWithShadow(textRenderer, Text.literal("Color:"), panelX + 20, panelY + 65, TEXT_COLOR);

            // Draw fields (handled by super.render)
        }
    }

    private void renderBackground(DrawContext context) {
        context.fill(0, 0, width, height, 0xC0101010); // Semi-transparent dark background
    }

    private boolean isMouseOverEntry(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle waypoint list clicks
        int waypointListX = width / 2 + 10;
        int waypointListY = 60;
        int listWidth = 190;
        int entryHeight = 30;

        int visibleCount = Math.min(entriesPerPage, waypointEntries.size() - listScrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            int index = i + listScrollOffset;
            if (index >= 0 && index < waypointEntries.size()) {
                int entryY = waypointListY + i * entryHeight;

                if (isMouseOverEntry((int)mouseX, (int)mouseY, waypointListX, entryY, listWidth - 20, entryHeight)) {
                    // Select entry
                    selectedEntry = waypointEntries.get(index);
                    updateButtonStates();

                    // Check if visibility toggle clicked
                    int visibilityX = waypointListX + listWidth - 35;
                    if (mouseX >= visibilityX && mouseX <= visibilityX + 15 &&
                            mouseY >= entryY + 5 && mouseY <= entryY + 20) {
                        // Toggle visibility
                        selectedEntry.waypoint.setVisible(!selectedEntry.waypoint.isVisible());
                        return true;
                    }

                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private class WaypointListEntry {
        final Waypoint waypoint;

        WaypointListEntry(Waypoint waypoint) {
            this.waypoint = waypoint;
        }
    }

    private class RangeSliderWidget extends SliderWidget {
        private final double minValue;
        private final double maxValue;
        private final Consumer<Number> valueConsumer;

        public RangeSliderWidget(int x, int y, int width, int height, Text message,
                                 double value, double minValue, double maxValue, Consumer<Number> valueConsumer) {
            super(x, y, width, height, message, value);
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.valueConsumer = valueConsumer;
        }

        @Override
        protected void updateMessage() {
            // Message is handled when setting value
        }

        @Override
        protected void applyValue() {
            double scaledValue = minValue + (maxValue - minValue) * value;

            // Round to integer if the range is large
            if (maxValue - minValue > 10) {
                valueConsumer.accept((int) Math.round(scaledValue));
            } else {
                // Keep decimal precision
                valueConsumer.accept((float) scaledValue);
            }
        }
    }
}