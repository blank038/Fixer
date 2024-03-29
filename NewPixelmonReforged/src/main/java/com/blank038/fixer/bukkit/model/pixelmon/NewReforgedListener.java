package com.blank038.fixer.bukkit.model.pixelmon;

import com.aystudio.core.forge.event.ForgeEvent;
import com.blank038.fixer.bukkit.Fixer;
import com.blank038.fixer.bukkit.model.pixelmon.stats.FixStatsEnum;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.events.CaptureEvent;
import com.pixelmonmod.pixelmon.api.events.LevelUpEvent;
import com.pixelmonmod.pixelmon.api.events.battles.AttackEvent;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.BaseStats;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.BaseStatsLoader;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.StatsType;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.evolution.types.LevelingEvolution;
import com.pixelmonmod.pixelmon.enums.heldItems.EnumHeldItems;
import com.pixelmonmod.pixelmon.storage.PlayerPartyStorage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * @author Blank038
 * @since 2021-05-14
 */
public class NewReforgedListener extends ReforgedListener implements Listener {

    public NewReforgedListener() {
        if (Fixer.getConfiguration().getBoolean("message.pixelmon.form_stats.enable")) {
            // 增加数据
            try {
                StatsType[] statsTypes = StatsType.getStatValues();
                for (FixStatsEnum fse : FixStatsEnum.values()) {
                    BaseStats formStats = BaseStatsLoader.getBaseStatsFromAssets(fse.getSpecies());
                    // 设置种族值
                    for (int i = 0; i < 6; i++) {
                        formStats.stats.replace(statsTypes[i], fse.getStats(i));
                    }
                    // 设置属性
                    formStats.types = new ArrayList<>(fse.getTypes());
                    // 将基础数据类增加至形态集合
                    fse.getSpecies().getBaseStats().forms = new LinkedHashMap<>();
                    fse.getSpecies().getBaseStats().forms.put(1, formStats);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    @EventHandler
    public void onForge(ForgeEvent event) {
        if (event.getForgeEvent() instanceof AttackEvent.Use && skillCopyItem) {
            AttackEvent.Use e = (AttackEvent.Use) event.getForgeEvent();
            if (e.user.getPlayerOwner() != null && e.target.getPlayerOwner() == null && !e.target.isMega) {
                if ("Covet".equals(e.getAttack().getMove().getAttackName()) && e.target.hasHeldItem()
                        && BATTLE_CONTROLLER_MAP.remove(e.target.getPokemonUUID().toString()) != null) {
                    e.target.setNewHeldItem(null);
                    Fixer.getInstance().getLogger().info("尝试防止玩家技能复制物品, 触发玩家: " + e.user.getPlayerOwner().getName());
                } else if ("Bestow".equals(e.getAttack().getMove().getAttackName())) {
                    BATTLE_CONTROLLER_MAP.put(e.target.getPokemonUUID().toString(), e.target.bc);
                }
            }
        } else if (event.getForgeEvent() instanceof CaptureEvent.StartCapture && Fixer.getConfiguration().getBoolean("message.pixelmon.combo.enable")) {
            CaptureEvent.StartCapture e = (CaptureEvent.StartCapture) event.getForgeEvent();
            PlayerPartyStorage storage = Pixelmon.storageManager.getParty(e.player);
            storage.transientData.captureCombo.clearCombo();
        } else if (event.getForgeEvent() instanceof LevelUpEvent && Fixer.getConfiguration().getBoolean("message.pixelmon.evolution.enable")) {
            LevelUpEvent e = (LevelUpEvent) event.getForgeEvent();
            if ((e.pokemon.getHeldItem() == null || e.pokemon.getHeldItem().getHeldItemType() != EnumHeldItems.everStone)
                    && !e.pokemon.getPokemon().getEvolutions(LevelingEvolution.class).isEmpty()) {
                EntityPixelmon pixelmon = e.pokemon.getPokemon().getOrSpawnPixelmon(e.pokemon.getPlayerOwner());
                for (LevelingEvolution evolution : e.pokemon.getPokemon().getEvolutions(LevelingEvolution.class)) {
                    if (evolution.canEvolve(pixelmon, e.newLevel)) {
                        e.setCanceled(true);
                        if (evolution.doEvolution(pixelmon) && Fixer.getConfiguration().getBoolean("message.pixelmon.evolution.new_level")) {
                            pixelmon.getPokemonData().getLevelContainer().setLevel(e.newLevel);
                            break;
                        }
                    }
                }
            }
        }
    }
}
