package dev.wolfieboy09.sdironspells;

import dev.wolfieboy09.sd5j.button.NamedButton;
import dev.wolfieboy09.sd5j.layout.DeckPaginator;
import dev.wolfieboy09.sd5j.layout.DeckSurface;
import dev.wolfieboy09.streamdecked.client.DeckTextures;
import dev.wolfieboy09.streamdecked.plugin.DeckLayoutRegistry;
import dev.wolfieboy09.streamdecked.plugin.StreamDeckedPlugin;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.network.casting.QuickCastPacket;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SDIronSpellsPlugin implements StreamDeckedPlugin {
    private static final String ID = "spells";

    private static DeckSurface live;

    private static String appliedSignature = "";

    @Override
    public void registerLayouts(DeckLayoutRegistry registry) {
        registry.register(SDIronSpells.id(ID),
                DeckTextures.itemOrPlaceholder(ItemRegistry.COPPER_SPELL_BOOK.get()),
                SDIronSpellsPlugin::populate);
    }

    private static void populate(DeckSurface surface) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        paginate(surface, knownSpells(), player, false).build().getFirst().forEach(surface::setButton);
    }

    static void onDeckReady(DeckSurface surface, @Nullable Player player) {
        live = surface;
        appliedSignature = "";
        refresh(player);
    }

    static void onDeckDisconnected() {
        live = null;
        appliedSignature = "";
    }

    static void refresh(@Nullable Player player) {
        if (live == null || player == null) return;
        if (!SDIronSpells.id(ID).toString().equals(live.currentFolderId())) return;
        sync(live, player);
    }

    private static void sync(DeckSurface surface, Player player) {
        List<AbstractSpell> all = knownSpells();
        String signature = signature(all);
        DeckPaginator<AbstractSpell> pages = paginate(surface, all, player, true);

        if (signature.equals(appliedSignature) && showing(surface, pages)) return;
        appliedSignature = signature;

        pages.refresh(surface);
    }

    private static boolean showing(DeckSurface surface, DeckPaginator<AbstractSpell> pages) {
        if (surface.pageCount() != pages.pageCount()) return false;
        int index = Math.min(surface.currentPageIndex(), pages.pageCount() - 1);
        Set<String> expected = new LinkedHashSet<>();
        for (AbstractSpell spell : pages.pages().get(index)) expected.add(spellName(spell));
        return new LinkedHashSet<>(surface.buttonNames()).equals(expected);
    }

    private static DeckPaginator<AbstractSpell> paginate(DeckSurface surface, List<AbstractSpell> spells, Player player, boolean navigation) {
        DeckPaginator<AbstractSpell> pages =
                DeckPaginator.of(surface.model(), spells, spell -> button(spell, player));
        if (!navigation) return pages.noNavigation();
        pages.previousIfRoom();
        return pages;
    }

    private static List<AbstractSpell> knownSpells() {
        var selection = ClientMagicData.getSpellSelectionManager();
        if (selection == null) return List.of();

        List<AbstractSpell> equipped = new ArrayList<>(selection.getSpellCount());
        for (var option : selection.getAllSpells()) {
            equipped.add(option.spellData.getSpell());
        }
        return equipped;
    }

    private static NamedButton button(AbstractSpell spell, Player player) {
        return new NamedButton(spellName(spell),
                DeckTextures.loadOrPlaceholder(spell.getSpellIconResource()),
                () -> cast(spell)).setCaption(caption(spell, player));
    }

    private static String caption(AbstractSpell spell, @Nullable Player player) {
        return player == null ? spell.getSpellName() : spell.getDisplayName(player).getString();
    }

    private static String spellName(AbstractSpell spell) {
        return spell.getSpellResource().toString();
    }

    private static String signature(List<AbstractSpell> spells) {
        StringBuilder sb = new StringBuilder();
        for (AbstractSpell spell : spells) sb.append(spellName(spell)).append(';');
        return sb.toString();
    }

    private static void cast(AbstractSpell spell) {
        var selection = ClientMagicData.getSpellSelectionManager();
        if (selection == null) return;

        int slot = -1;
        for (var option : selection.getAllSpells()) {
            if (option.spellData.getSpell().equals(spell)) {
                slot = option.globalIndex;
                break;
            }
        }

        if (slot == -1) {
            SDIronSpells.LOGGER.warn("{} left the spell bar before it could be cast", spellName(spell));
            return;
        }

        PacketDistributor.sendToServer(new QuickCastPacket(slot));
    }
}
