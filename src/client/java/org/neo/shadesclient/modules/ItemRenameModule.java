package org.neo.shadesclient.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import org.neo.shadesclient.client.ShadesClient;
import org.neo.shadesclient.qolitems.Module;
import org.neo.shadesclient.qolitems.ModuleCategory;
import org.neo.shadesclient.qolitems.ModuleConfigGUI;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemRenameModule extends Module {
    private final MinecraftClient client = MinecraftClient.getInstance();

    // Map to store custom names and colors for items using their UUID as identifier
    private final Map<UUID, RenamedItem> renamedItems = new HashMap<>();

    // Currently selected color for the rename operation
    private Formatting currentColor = Formatting.WHITE;

    // Currently editing item
    private ItemStack currentItem = null;

    public ItemRenameModule(String name, String description, ModuleCategory category) {
        super(name, description, category);
    }

    @Override
    protected void onEnable() {
        ShadesClient.LOGGER.info("Item Rename module enabled");
    }

    @Override
    protected void onDisable() {
        ShadesClient.LOGGER.info("Item Rename module disabled");
    }

    @Override
    public boolean hasConfigScreen() {
        return true;
    }

    @Override
    public void openConfigScreen() {
        Screen currentScreen = client.currentScreen;
        client.setScreen(new ItemRenameConfigGUI(currentScreen, this));
    }

    /**
     * Gets the current color selected for renaming
     */
    public Formatting getCurrentColor() {
        return currentColor;
    }

    /**
     * Sets the current color for renaming
     */
    public void setCurrentColor(Formatting color) {
        this.currentColor = color;
    }

    /**
     * Sets the current item being edited
     */
    public void setCurrentItem(ItemStack item) {
        this.currentItem = item;
    }

    /**
     * Gets the current item being edited
     */
    public ItemStack getCurrentItem() {
        return currentItem;
    }

    /**
     * Renames the currently selected item
     * @param newName The new name to give the item
     */
    public void renameCurrentItem(String newName) {
        if (currentItem == null || newName == null) {
            return;
        }

        // Get or create a unique identifier for this item
        UUID itemId = getItemId(currentItem);

        // Create styled text with the selected color
        Text displayName = Text.literal(newName).setStyle(Style.EMPTY.withColor(currentColor));

        // Store in our mapping
        renamedItems.put(itemId, new RenamedItem(newName, currentColor));

        ShadesClient.LOGGER.info("Renamed item to '" + newName + "' with color " + currentColor.getName());
    }

    /**
     * Resets the name of the currently selected item
     */
    public void resetCurrentItemName() {
        if (currentItem == null) {
            return;
        }

        UUID itemId = getItemId(currentItem);
        if (renamedItems.containsKey(itemId)) {
            renamedItems.remove(itemId);
            ShadesClient.LOGGER.info("Reset item name to default");
        }
    }

    /**
     * Gets a custom display name for an item if it exists
     * @param item The item stack to check
     * @return The custom text to display or null if no custom name exists
     */
    public Text getCustomDisplayName(ItemStack item) {
        if (!isEnabled() || item == null || item.isEmpty()) {
            return null;
        }

        UUID itemId = getItemId(item);
        RenamedItem renamedItem = renamedItems.get(itemId);

        if (renamedItem != null) {
            return Text.literal(renamedItem.name).setStyle(Style.EMPTY.withColor(renamedItem.color));
        }

        return null;
    }

    /**
     * Get a unique identifier for an item stack
     * This combines the item properties to create a reasonably unique ID
     */
    private UUID getItemId(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return new UUID(0L, 0L);
        }

        // Combine various item properties to create a "unique" identifier
        int hash = 17;
        hash = 31 * hash + item.getItem().hashCode();
        hash = 31 * hash + item.getDamage();

        // We can't check for NBT directly based on the available methods,
        // but we can use the item's raw ID and count in the hash
        hash = 31 * hash + item.getCount();

        // Create a UUID based on this hash
        long mostSigBits = hash * 31L;
        long leastSigBits = System.identityHashCode(item);

        return new UUID(mostSigBits, leastSigBits);
    }

    /**
     * Class to store renamed item information
     */
    private static class RenamedItem {
        public final String name;
        public final Formatting color;

        public RenamedItem(String name, Formatting color) {
            this.name = name;
            this.color = color;
        }
    }
}