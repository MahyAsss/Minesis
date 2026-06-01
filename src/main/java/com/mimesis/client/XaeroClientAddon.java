package com.mimesis.client;

import com.mimesis.entity.MimesisEntities;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Mod.EventBusSubscriber(modid = "mimesis", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class XaeroClientAddon {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final AtomicBoolean APPLIED = new AtomicBoolean(false);
    private static long nextRetryMs = 0L;

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info("Mimesis Xaero addon loaded. Waiting for world to patch Xaero radar at runtime.");
    }

    @Mod.EventBusSubscriber(modid = "mimesis", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class RuntimePatch {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || APPLIED.get()) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null) {
                return;
            }

            long now = System.currentTimeMillis();
            if (now < nextRetryMs) {
                return;
            }

            try {
                boolean ok = applyXaeroPatch();
                if (ok) {
                    APPLIED.set(true);
                    LOGGER.info("Mimesis Xaero addon: runtime patch applied successfully.");
                } else {
                    nextRetryMs = now + 3000L;
                }
            } catch (Throwable t) {
                nextRetryMs = now + 5000L;
                LOGGER.warn("Mimesis Xaero addon: runtime patch failed, will retry. {}", t.toString());
            }
        }
    }

    private static boolean applyXaeroPatch() throws Exception {
        Class<?> hudModCls = Class.forName("xaero.common.HudMod");
        Object hudMod = hudModCls.getField("INSTANCE").get(null);
        if (hudMod == null) {
            return false;
        }

        Object minimap = hudModCls.getMethod("getMinimap").invoke(hudMod);
        if (minimap == null) {
            return false;
        }
        Object fboRenderer = minimap.getClass().getMethod("getMinimapFBORenderer").invoke(minimap);
        if (fboRenderer == null) {
            return false;
        }
        Object radarRenderer = fboRenderer.getClass().getMethod("getEntityRadarRenderer").invoke(fboRenderer);
        if (radarRenderer == null) {
            return false;
        }

        boolean categoryPatched = patchPlayerCategoryToIncludeMimesis(hudModCls, hudMod);
        boolean iconPatched = patchMimesisIconCache(radarRenderer);

        return categoryPatched || iconPatched;
    }

    private static boolean patchPlayerCategoryToIncludeMimesis(Class<?> hudModCls, Object hudMod) {
        try {
            Object categoryManager = hudModCls.getMethod("getEntityRadarCategoryManager").invoke(hudMod);
            if (categoryManager == null) {
                return false;
            }

            Object root = categoryManager.getClass().getMethod("getEffectiveSyncedRootCategory").invoke(categoryManager);
            if (root == null) {
                root = categoryManager.getClass().getMethod("getRootCategory").invoke(categoryManager);
            }
            if (root == null) {
                return false;
            }

            Object playerCategory = findCategoryByNameContains(root, "player");
            if (playerCategory == null) {
                return false;
            }

            // Add mimesis entity type to player's ENTITY_TYPE include list.
            Class<?> listRuleTypesCls = Class.forName("xaero.hud.minimap.radar.category.rule.EntityRadarListRuleTypes");
            Object entityTypeRule = listRuleTypesCls.getField("ENTITY_TYPE").get(null);
            Object includeList;
            try {
                includeList = invokeSingleArgMethodByName(playerCategory, "getIncludeList", entityTypeRule);
            } catch (Throwable ignored) {
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

            @SuppressWarnings("unchecked")
            List<String> stringList = (List<String>) stringListField.get(includeList);
            @SuppressWarnings("unchecked")
            Set<Object> set = (Set<Object>) setField.get(includeList);

            boolean changed = false;
            String mimesisId = "mimesis:mimesis_entity";
            if (!stringList.contains(mimesisId)) {
                changed |= stringList.add(mimesisId);
            }
            Object mimesisType = MimesisEntities.MIMESIS_ENTITY.get();
            if (!set.contains(mimesisType)) {
                changed |= set.add(mimesisType);
            }

            if (changed) {
                LOGGER.info("Mimesis Xaero addon: added {} into Xaero player category include list.", mimesisId);
            }
            return true;
        } catch (Throwable t) {
            LOGGER.debug("Mimesis Xaero addon: category patch failed: {}", t.toString());
            return false;
        }
    }

    private static boolean patchMimesisIconCache(Object radarRenderer) {
        try {
            Field radarIconManagerField = findFieldRecursive(radarRenderer.getClass(), "radarIconManager");
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

            Field iconCacheField = findFieldRecursive(radarIconManager.getClass(), "iconCache");
            if (iconCacheField == null) {
                return false;
            }
            iconCacheField.setAccessible(true);
            Object iconCache = iconCacheField.get(radarIconManager);
            if (iconCache == null) {
                return false;
            }

            Method getEntityCache = iconCache.getClass().getMethod("getEntityCache", net.minecraft.world.entity.EntityType.class);
            Object entityCache = getEntityCache.invoke(iconCache, MimesisEntities.MIMESIS_ENTITY.get());
            if (entityCache == null) {
                return false;
            }

            Field storageField = entityCache.getClass().getDeclaredField("storage");
            storageField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Object, Object> storage = (Map<Object, Object>) storageField.get(entityCache);
            if (storage == null) {
                return false;
            }

            // Force every generated key to render as DOT icon.
            if (!storage.isEmpty()) {
                for (Object key : storage.keySet().toArray()) {
                    storage.put(key, dotIcon);
                }
            } else {
                // Seed one default key for early lookups.
                Class<?> radarIconKeyCls = Class.forName("xaero.hud.minimap.radar.icon.cache.id.RadarIconKey");
                Class<?> radarIconArmorCls = Class.forName("xaero.hud.minimap.radar.icon.cache.id.armor.RadarIconArmor");
                Constructor<?> keyCtor = radarIconKeyCls.getConstructor(Object.class, radarIconArmorCls);
                Object defaultKey = keyCtor.newInstance(null, null);
                entityCache.getClass().getMethod("add", radarIconKeyCls, dotIcon.getClass()).invoke(entityCache, defaultKey, dotIcon);
            }

            LOGGER.info("Mimesis Xaero addon: patched icon cache for mimesis entity type.");
            return true;
        } catch (Throwable t) {
            LOGGER.debug("Mimesis Xaero addon: icon cache patch failed: {}", t.toString());
            return false;
        }
    }

    private static Object findCategoryByNameContains(Object root, String needleLower) {
        try {
            Method getName = root.getClass().getMethod("getName");
            Method getDirectSubCategoryIterator = root.getClass().getMethod("getDirectSubCategoryIterator");
            Object name = getName.invoke(root);
            if (name instanceof String && ((String) name).toLowerCase().contains(needleLower)) {
                return root;
            }
            @SuppressWarnings("unchecked")
            Iterator<Object> it = (Iterator<Object>) getDirectSubCategoryIterator.invoke(root);
            while (it.hasNext()) {
                Object found = findCategoryByNameContains(it.next(), needleLower);
                if (found != null) {
                    return found;
                }
            }
        } catch (Throwable ignored) {
            return null;
        }
        return null;
    }

    private static Field findFieldRecursive(Class<?> cls, String name) {
        Class<?> cur = cls;
        while (cur != null) {
            try {
                return cur.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                cur = cur.getSuperclass();
            }
        }
        return null;
    }

    private static Object invokeSingleArgMethodByName(Object target, String methodName, Object arg) throws Exception {
        Method[] methods = target.getClass().getMethods();
        for (Method m : methods) {
            if (m.getName().equals(methodName) && m.getParameterCount() == 1) {
                try {
                    return m.invoke(target, arg);
                } catch (IllegalArgumentException ignored) {
                    // try next overload
                }
            }
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + methodName + "(1 arg)");
    }
}
