/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.world.entity.EntityType
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.event.TickEvent$ClientTickEvent
 *  net.minecraftforge.event.TickEvent$Phase
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package com.mimesis.client;

import com.mimesis.entity.MimesisEntities;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid="mimesis", value={Dist.CLIENT}, bus=Mod.EventBusSubscriber.Bus.MOD)
public class XaeroClientAddon {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final AtomicBoolean APPLIED = new AtomicBoolean(false);
    private static long nextRetryMs = 0L;

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info("Mimesis Xaero addon loaded. Waiting for world to patch Xaero radar at runtime.");
    }

    private static boolean applyXaeroPatch() throws Exception {
        Class<?> hudModCls = Class.forName("xaero.common.HudMod");
        Object hudMod = hudModCls.getField("INSTANCE").get(null);
        if (hudMod == null) {
            return false;
        }
        Object minimap = hudModCls.getMethod("getMinimap", new Class[0]).invoke(hudMod, new Object[0]);
        if (minimap == null) {
            return false;
        }
        Object fboRenderer = minimap.getClass().getMethod("getMinimapFBORenderer", new Class[0]).invoke(minimap, new Object[0]);
        if (fboRenderer == null) {
            return false;
        }
        Object radarRenderer = fboRenderer.getClass().getMethod("getEntityRadarRenderer", new Class[0]).invoke(fboRenderer, new Object[0]);
        if (radarRenderer == null) {
            return false;
        }
        boolean categoryPatched = XaeroClientAddon.patchPlayerCategoryToIncludeMimesis(hudModCls, hudMod);
        boolean iconPatched = XaeroClientAddon.patchMimesisIconCache(radarRenderer);
        return categoryPatched || iconPatched;
    }

    private static boolean patchPlayerCategoryToIncludeMimesis(Class<?> hudModCls, Object hudMod) {
        try {
            Object mimesisType;
            Object includeList;
            Object categoryManager = hudModCls.getMethod("getEntityRadarCategoryManager", new Class[0]).invoke(hudMod, new Object[0]);
            if (categoryManager == null) {
                return false;
            }
            Object root = categoryManager.getClass().getMethod("getEffectiveSyncedRootCategory", new Class[0]).invoke(categoryManager, new Object[0]);
            if (root == null) {
                root = categoryManager.getClass().getMethod("getRootCategory", new Class[0]).invoke(categoryManager, new Object[0]);
            }
            if (root == null) {
                return false;
            }
            Object playerCategory = XaeroClientAddon.findCategoryByNameContains(root, "player");
            if (playerCategory == null) {
                return false;
            }
            Class<?> listRuleTypesCls = Class.forName("xaero.hud.minimap.radar.category.rule.EntityRadarListRuleTypes");
            Object entityTypeRule = listRuleTypesCls.getField("ENTITY_TYPE").get(null);
            try {
                includeList = XaeroClientAddon.invokeSingleArgMethodByName(playerCategory, "getIncludeList", entityTypeRule);
            }
            catch (Throwable ignored) {
                includeList = null;
            }
            if (includeList == null) {
                return false;
            }
            Class<?> listRuleCls = Class.forName("xaero.hud.category.rule.ObjectCategoryListRule");
            Field stringListField = listRuleCls.getDeclaredField("stringList");
            Field setField = listRuleCls.getDeclaredField("set");
            stringListField.setAccessible(true);
            setField.setAccessible(true);
            List stringList = (List)stringListField.get(includeList);
            Set set = (Set)setField.get(includeList);
            boolean changed = false;
            String mimesisId = "mimesis:mimesis_entity";
            if (!stringList.contains(mimesisId)) {
                changed |= stringList.add(mimesisId);
            }
            if (!set.contains(mimesisType = MimesisEntities.MIMESIS_ENTITY.get())) {
                changed |= set.add(mimesisType);
            }
            if (changed) {
                LOGGER.info("Mimesis Xaero addon: added {} into Xaero player category include list.", (Object)mimesisId);
            }
            return true;
        }
        catch (Throwable t) {
            LOGGER.debug("Mimesis Xaero addon: category patch failed: {}", (Object)t.toString());
            return false;
        }
    }

    private static boolean patchMimesisIconCache(Object radarRenderer) {
        try {
            Field radarIconManagerField = XaeroClientAddon.findFieldRecursive(radarRenderer.getClass(), "radarIconManager");
            if (radarIconManagerField == null) {
                return false;
            }
            radarIconManagerField.setAccessible(true);
            Object radarIconManager = radarIconManagerField.get(radarRenderer);
            if (radarIconManager == null) {
                return false;
            }
            Object dotIcon = radarIconManager.getClass().getField("DOT").get(null);
            if (dotIcon == null) {
                return false;
            }
            Field iconCacheField = XaeroClientAddon.findFieldRecursive(radarIconManager.getClass(), "iconCache");
            if (iconCacheField == null) {
                return false;
            }
            iconCacheField.setAccessible(true);
            Object iconCache = iconCacheField.get(radarIconManager);
            if (iconCache == null) {
                return false;
            }
            Method getEntityCache = iconCache.getClass().getMethod("getEntityCache", EntityType.class);
            Object entityCache = getEntityCache.invoke(iconCache, MimesisEntities.MIMESIS_ENTITY.get());
            if (entityCache == null) {
                return false;
            }
            Field storageField = entityCache.getClass().getDeclaredField("storage");
            storageField.setAccessible(true);
            Map storage = (Map)storageField.get(entityCache);
            if (storage == null) {
                return false;
            }
            if (!storage.isEmpty()) {
                for (Object key : storage.keySet().toArray()) {
                    storage.put(key, dotIcon);
                }
            } else {
                Class<?> radarIconKeyCls = Class.forName("xaero.hud.minimap.radar.icon.cache.id.RadarIconKey");
                Class<?> radarIconArmorCls = Class.forName("xaero.hud.minimap.radar.icon.cache.id.armor.RadarIconArmor");
                Constructor<?> keyCtor = radarIconKeyCls.getConstructor(Object.class, radarIconArmorCls);
                Object defaultKey = keyCtor.newInstance(null, null);
                entityCache.getClass().getMethod("add", radarIconKeyCls, dotIcon.getClass()).invoke(entityCache, defaultKey, dotIcon);
            }
            LOGGER.info("Mimesis Xaero addon: patched icon cache for mimesis entity type.");
            return true;
        }
        catch (Throwable t) {
            LOGGER.debug("Mimesis Xaero addon: icon cache patch failed: {}", (Object)t.toString());
            return false;
        }
    }

    private static Object findCategoryByNameContains(Object root, String needleLower) {
        try {
            Method getName = root.getClass().getMethod("getName", new Class[0]);
            Method getDirectSubCategoryIterator = root.getClass().getMethod("getDirectSubCategoryIterator", new Class[0]);
            Object name = getName.invoke(root, new Object[0]);
            if (name instanceof String && ((String)name).toLowerCase().contains(needleLower)) {
                return root;
            }
            Iterator it = (Iterator)getDirectSubCategoryIterator.invoke(root, new Object[0]);
            while (it.hasNext()) {
                Object found = XaeroClientAddon.findCategoryByNameContains(it.next(), needleLower);
                if (found == null) continue;
                return found;
            }
        }
        finally {
            return null;
        }
        {
        }
    }

    private static Field findFieldRecursive(Class<?> cls, String name) {
        for (Class<?> cur = cls; cur != null; cur = cur.getSuperclass()) {
            try {
                return cur.getDeclaredField(name);
            }
            catch (NoSuchFieldException ignored) {
                continue;
            }
        }
        return null;
    }

    private static Object invokeSingleArgMethodByName(Object target, String methodName, Object arg) throws Exception {
        Method[] methods;
        for (Method m : methods = target.getClass().getMethods()) {
            if (!m.getName().equals(methodName) || m.getParameterCount() != 1) continue;
            try {
                return m.invoke(target, arg);
            }
            catch (IllegalArgumentException illegalArgumentException) {
                // empty catch block
            }
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + methodName + "(1 arg)");
    }

    @Mod.EventBusSubscriber(modid="mimesis", value={Dist.CLIENT}, bus=Mod.EventBusSubscriber.Bus.FORGE)
    public static class RuntimePatch {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || APPLIED.get()) {
                return;
            }
            Minecraft mc = Minecraft.m_91087_();
            if (mc == null || mc.f_91073_ == null) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now < nextRetryMs) {
                return;
            }
            try {
                boolean ok = XaeroClientAddon.applyXaeroPatch();
                if (ok) {
                    APPLIED.set(true);
                    LOGGER.info("Mimesis Xaero addon: runtime patch applied successfully.");
                } else {
                    nextRetryMs = now + 3000L;
                }
            }
            catch (Throwable t) {
                nextRetryMs = now + 5000L;
                LOGGER.warn("Mimesis Xaero addon: runtime patch failed, will retry. {}", (Object)t.toString());
            }
        }
    }
}

