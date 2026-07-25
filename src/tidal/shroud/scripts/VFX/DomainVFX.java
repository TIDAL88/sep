package tidal.shroud.scripts.VFX;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.dark.shaders.util.ShaderLib;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.EnumSet;

public class DomainVFX extends BaseCombatLayeredRenderingPlugin {
    private static final String DOMAIN_BACKGROUND_PRIMARY = "graphics/tidal/domain/hyperspace1.png";
    private static final String DOMAIN_SHADER = "data/shaders/td_primordialSeaShader.shader";

    private static final Color BOSS_BASE_COLOR = new Color(178, 36, 69, 255);
    private static final Color BOSS_RING_COLOR = new Color(255, 70, 95, 220);
    private static int shader = 0;
    private static boolean shaderInit = false;

    private final ShipAPI ship;
    private final Color baseColor;
    private final Color ringColor;
    private final boolean mapScale;
    private final SpriteAPI background;
    

    private float targetRadius = 0f;
    private float currentRadius = 0f;
    private float effectLevel = 0f;
    private float time = 0f;
    private boolean collapsing = false;
    private boolean expired = false;

    public static DomainVFX createBoss(ShipAPI ship) {
        return new DomainVFX(ship, BOSS_BASE_COLOR, BOSS_RING_COLOR, true);
    }

    public DomainVFX(ShipAPI ship, Color baseColor, Color ringColor, boolean mapScale) {
        this.ship = ship;
        this.baseColor = baseColor;
        this.ringColor = ringColor;
        this.mapScale = mapScale;
        this.background = loadSpriteOrFallback();
        ensureShaderLoaded();
    }

    private static void ensureShaderLoaded() {
        if (shaderInit) return;
        shaderInit = true;
        try {
            shader = ShaderLib.loadShader(Global.getSettings().loadText("data/shaders/baseVertex.shader"), Global.getSettings().loadText(DOMAIN_SHADER));
            if (shader != 0) {
                GL20.glUseProgram(shader);
                GL20.glUniform1i(GL20.glGetUniformLocation(shader, "tex"), 0);
                GL20.glUseProgram(0);
            }
        } catch (Throwable ignored) {
            shader = 0;
        }
    }

    private SpriteAPI loadSpriteOrFallback() {
        SpriteAPI sprite = tryLoadSprite(DomainVFX.DOMAIN_BACKGROUND_PRIMARY);
        if (sprite != null) return sprite;
        sprite = tryLoadSprite("graphics/backgrounds/hyperspace1.jpg");
        if (sprite != null) return sprite;
        return Global.getSettings().getSprite("misc", "nebula_particles");
    }

    private SpriteAPI tryLoadSprite(String path) {
        if (path == null || path.isEmpty()) return null;
        try {
            Global.getSettings().loadTexture(path);
            return Global.getSettings().getSprite(path);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public void setActiveState(float effectLevel, float radius) {
        collapsing = false;
        this.effectLevel = Math.max(0f, Math.min(1f, effectLevel));
        float desiredRadius = mapScale ? computeMapRadius() : Math.max(0f, radius);
        targetRadius = desiredRadius * this.effectLevel;
    }

    public void setMapActiveState(float effectLevel) {
        setActiveState(effectLevel, 0f);
    }

    public void beginCollapse() {
        collapsing = true;
        targetRadius = 0f;
    }

    private float computeMapRadius() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.getMapWidth() <= 0f || engine.getMapHeight() <= 0f) {
            return 12000f;
        }
        float halfWidth = engine.getMapWidth() * 0.5f;
        float halfHeight = engine.getMapHeight() * 0.5f;
        return (float) Math.sqrt((halfWidth * halfWidth) + (halfHeight * halfHeight)) + 2000f;
    }

    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;
        if (ship == null || !ship.isAlive()) {
            expired = true;
            return;
        }
        time += amount;
        float speed = collapsing ? 8f : 10f;
        currentRadius += (targetRadius - currentRadius) * Math.min(1f, amount * speed);
        if (collapsing && currentRadius <= 5f) expired = true;
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (expired || viewport == null || ship == null) return;
        if (layer == CombatEngineLayers.JUST_BELOW_WIDGETS) {
            renderShaderMask();
            return;
        }
        if (currentRadius <= 5f) return;
        if (layer == CombatEngineLayers.BELOW_PLANETS) {
            startStencil(ship.getLocation(), currentRadius, mapScale ? 160 : 96);
            renderDomainBackdrop(viewport);
            endStencil();
            return;
        }
        if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER) {
            renderDomainBorder(ship.getLocation(), currentRadius, viewport.getAlphaMult());
        }
    }

    private void renderShaderMask() {
        if (shader == 0 || ShaderLib.getScreenTexture() == 0 || currentRadius <= 5f) return;
        Vector2f worldCenter = ship.getLocation();
        Vector2f worldEdge = Misc.getUnitVectorAtDegreeAngle(0f);
        worldEdge.scale(currentRadius);
        worldEdge = Vector2f.add(worldCenter, worldEdge, new Vector2f());
        Vector2f screenCenter = ShaderLib.transformWorldToScreen(worldCenter);
        Vector2f screenEdge = ShaderLib.transformWorldToScreen(worldEdge);
        float radiusPx = Misc.getDistance(screenCenter, screenEdge);
        Vector2f centerUV = ShaderLib.transformScreenToUV(screenCenter);
        ShaderLib.beginDraw(shader);
        GL20.glUniform1f(GL20.glGetUniformLocation(shader, "intensity"), Math.max(0.35f, effectLevel));
        GL20.glUniform2f(GL20.glGetUniformLocation(shader, "centerUV"), centerUV.x, centerUV.y);
        GL20.glUniform1f(GL20.glGetUniformLocation(shader, "radiusPx"), radiusPx);
        GL20.glUniform1f(GL20.glGetUniformLocation(shader, "screenWidth"), Global.getSettings().getScreenWidthPixels());
        GL20.glUniform1f(GL20.glGetUniformLocation(shader, "screenHeight"), Global.getSettings().getScreenHeightPixels());
        GL20.glUniform1f(GL20.glGetUniformLocation(shader, "visibleU"), ShaderLib.getVisibleU());
        GL20.glUniform1f(GL20.glGetUniformLocation(shader, "visibleV"), ShaderLib.getVisibleV());
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, ShaderLib.getScreenTexture());
        GL11.glDisable(GL11.GL_BLEND);
        ShaderLib.screenDraw(ShaderLib.getScreenTexture(), GL13.GL_TEXTURE0);
        ShaderLib.exitDraw();
    }

    private void renderDomainBackdrop(ViewportAPI viewport) {
        float width = viewport.getVisibleWidth();
        float height = viewport.getVisibleHeight();
        float x = viewport.getLLX() + (width * 0.5f);
        float y = viewport.getLLY() + (height * 0.5f);
        float alpha = viewport.getAlphaMult() * Math.max(0.25f, effectLevel);
        float pulseA = 1.16f + (float) Math.sin(time * 0.45f) * 0.03f;
        float pulseB = 1.26f + (float) Math.cos(time * 0.35f) * 0.02f;

        background.setNormalBlend();
        background.setColor(baseColor);
        background.setAlphaMult(alpha * 0.85f);
        background.setSize(width * pulseA, height * pulseA);
        background.setAngle((float) Math.sin(time * 0.05f) * 1.5f);
        background.renderAtCenter(x, y);

        background.setAlphaMult(alpha * 0.28f);
        background.setSize(width * pulseB, height * pulseB);
        background.setAngle((float) Math.cos(time * 0.04f) * 1.5f);
        background.renderAtCenter(x, y);
        
    }

    private void startStencil(Vector2f center, float radius, int segments) {
        GL11.glClearStencil(0);
        GL11.glStencilMask(0xff);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glColorMask(false, false, false, false);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xff);
        GL11.glStencilMask(0xff);
        GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_REPLACE, GL11.GL_REPLACE);
        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0; i <= segments; i++) {
            double angle = (2d * Math.PI * i) / segments;
            GL11.glVertex2d(center.x + Math.cos(angle) * radius, center.y + Math.sin(angle) * radius);
        }
        GL11.glEnd();
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        GL11.glColorMask(true, true, true, true);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xff);
    }

    private void endStencil() {
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    private void renderDomainBorder(Vector2f center, float radius, float alphaMult) {
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        float alpha = (ringColor.getAlpha() / 255f) * alphaMult;
        GL11.glColor4f(ringColor.getRed() / 255f, ringColor.getGreen() / 255f, ringColor.getBlue() / 255f, alpha);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        int segments = mapScale ? 160 : 100;
        for (int i = 0; i <= segments; i++) {
            double angle = (2d * Math.PI * i) / segments;
            GL11.glVertex2d(center.x + Math.cos(angle) * radius, center.y + Math.sin(angle) * radius);
        }
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    @Override
    public float getRenderRadius() {
        return 100000f;
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.BELOW_PLANETS, CombatEngineLayers.ABOVE_SHIPS_LAYER, CombatEngineLayers.JUST_BELOW_WIDGETS);
    }
}
