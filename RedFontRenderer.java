package cn.asone.endless.ui.font;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class RedFontRenderer {
    public final Font font;
    private String asciiCharacters = "\u00c0\u00c1\u00c2\u00c8\u00ca\u00cb\u00cd\u00d3\u00d4\u00d5\u00da\u00df\u00e3\u00f5\u011f\u0130\u0131\u0152\u0153\u015e\u015f\u0174\u0175\u017e\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u03b2\u0393\u03c0\u03a3\u03c3\u03bc\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u2205\u2208\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u0000";

    /**
     * 储存字符图片及相应信息的数组. <p>
     * 数组的大小为 65535, char所储存的字符对应 ascii 码最大值也为 65535. <p>
     * 由于字符数量庞大, 在初始化的时候就加载所有字符图片是不必要的, 因此在调用时要先进行非空检查.<p>
     * 需要注意的是, 如果在其他线程调用此类中含有OpenGL相关的方法时, chars并不会立即更新<p>
     *
     * @see #getFontChar(int)
     */
    private final FontChar[] chars = new FontChar[65535];
    private final FontMetrics fontMetrics;
    /**
     * 当前字体渲染出的单个字符图片的高度.<p>
     * 在 {@link cn.asone.endless.ui.font.RedFontRenderer#generateCharImage(int)}
     * 中保证所有字符图片高度均为此值.
     * </p>
     */
    private final int fontImageHeight;
    private final float scale = 0.5F;
    public int fontHeight;

    public RedFontRenderer(Font font) {
        this.font = font;
        Graphics2D graphics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setFont(font);
        fontMetrics = graphics.getFontMetrics();
        if (fontMetrics.getHeight() <= 0)
            fontImageHeight = font.getSize();
        else
            fontImageHeight = fontMetrics.getHeight();

        //从空格至波浪号预加载字符图片
        for (int charCode = 32; charCode <= 126; charCode++) {
            chars[charCode] = new FontChar((char) charCode, generateCharImage(charCode));
        }
        fontHeight = (int) (fontImageHeight * scale);
    }

    public int drawString(String text, double x, double y, int color) {
        GL11.glPushMatrix();
        GL11.glTranslated(x, y - 1, 0D);
        GL11.glScalef(scale, scale, scale);
        RenderUtils.quickGLColor(color);
        int width = 0;
        for (char ch : text.toCharArray()) {
            int singleWidth = drawCharImage(ch, width);
            width += singleWidth;
            //GL11.glTranslated(singleWidth, 0D, 0D);
        }
        GL11.glPopMatrix();
        return (int) (width * scale);
    }

    public int drawChar(char charAt, double x, double y, int color) {
        GL11.glPushMatrix();
        GL11.glTranslated(x, y - 1.5, 0D);
        GL11.glScalef(scale, scale, scale);
        RenderUtils.quickGLColor(color);
        int width = drawCharImage(charAt, 0);
        GL11.glPopMatrix();
        return (int) (width * scale);
    }

    public int getStringWidth(String text) {
        int width = 0;
        for (char ch : text.toCharArray()) {
            FontChar c = getFontChar(ch);
            if (c != null) {
                width += c.getTex().width;
            }
        }
        return (int) (width * scale);
    }

    public int getCharWidth(char charAt) {
        FontChar c = getFontChar(charAt);
        if (c == null)
            return 0;
        return (int) (c.getTex().width * scale);
    }

    /**
     * 将字体图片渲染到屏幕上
     */
    private int drawCharImage(int charAt, int offset) {
        FontChar fontChar = getFontChar(charAt);
        if (fontChar == null) {
            return 0;
        }
        GlStateManager.bindTexture(fontChar.getTex().getGlTextureId());
        Gui.drawModalRectWithCustomSizedTexture(
                offset,
                0,
                offset,
                0F,
                fontChar.getTex().width,
                fontImageHeight,
                fontChar.getTex().width,
                fontImageHeight
        );
        return fontChar.getTex().width;
    }

    /**
     * 根据给定的字体将字符渲染为图片
     *
     * @param charAt 将要渲染的字符
     * @return 字符图片
     */
    private BufferedImage generateCharImage(int charAt) {
        //+1:在字符图片右留出1px的空隙
        int charWidth = fontMetrics.charWidth(charAt) + 1;
        if (charWidth <= 0)
            charWidth = 1;
        BufferedImage charImage = new BufferedImage(charWidth, fontImageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = charImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setFont(font);
        graphics.setColor(Color.WHITE);
        graphics.drawString(String.valueOf((char) charAt), 0, fontMetrics.getAscent());

        /*
         * 接下来的代码所做的工作是将字体不支持的字符替换为 Minecraft 自带的字符图片
         */

        //空格不需要转换
        if (charAt == 32)
            return charImage;
        if (font.canDisplay(charAt))
            /*
             * 判断字体图片是否为空白图片
             */
            for (int m = 0; m < charImage.getWidth(); m++) {
                for (int n = 0; n < charImage.getHeight(); n++) {
                    if (charImage.getRGB(m, n) != 0)
                        return charImage;
                }
            }

        try {
            /*
             * 从原版FontRenderer获取字符图片
             */
            int index = asciiCharacters.indexOf(charAt);
            /*
             * Unicode style
             */
            if (index == -1) {
                ResourceLocation location = Minecraft.getMinecraft().fonts.getUnicodePageLocation(charAt / 256);
                BufferedImage globalImage = TextureUtil.readBufferedImage(Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream());

                /*
                 * 当前字符对应的图片的有效部分的起始x坐标.
                 * 由于不是所有字符都会占满16×16的空间, 图片实际有效部分周围会有空白.
                 * 此值表示当前字符对于标准16×16起始的x坐标至此字符图片有效部分的起始x坐标的差.
                 */
                int xOffset = Minecraft.getMinecraft().fonts.glyphWidth[charAt] >>> 4 & 0xF;

                int k = Minecraft.getMinecraft().fonts.glyphWidth[charAt] & 15;
                int f1 = k + 1;
                /*
                 * 图片中所在列
                 */
                int xPos = charAt % 16 * 16 + xOffset;
                /*
                 * 图片中所在行
                 */
                int yPos = (charAt & 0xFF / 16 * 16);
                /*
                 * 字符对应图片宽度
                 */
                int width = f1 - xOffset;
                /*
                 * 字符图片对应高度
                 *
                 * 如果当前自定义字体渲染出的标准图片高度小于16(mc字体单个字符图片的高度),
                 * 则将值设为16以保证能够完整渲染图片并忽略可能的后果(因为当自定义字体图片高度小于16
                 * 时一般实际的渲染效果都不尽人意)
                 */
                int height = Math.max(fontImageHeight, 16);
                int startY = (height - 16) / 2;
                BufferedImage image = new BufferedImage(width + 2, height, BufferedImage.TYPE_INT_ARGB);
                for (int m = 0; m < width; m++) {
                    for (int n = 0; n < 16; n++) {
                        image.setRGB(m + 1, startY + n, globalImage.getRGB(m + xPos, n + yPos));
                    }
                }
                return image;
            } else {
                /*
                 * 从 ascii.png 获取图片
                 */
                int xPos = index % 16 * 8;
                int yPos = index / 16 * 8;
                //float width = Minecraft.getMinecraft().fonts.charWidthFloat[index];
                ResourceLocation location = Minecraft.getMinecraft().fonts.locationFontTexture;
                BufferedImage globalImage = TextureUtil.readBufferedImage(Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream());
                int height = Math.max(fontImageHeight, 16);
                int startY = (height - 16) / 2;
                BufferedImage image = new BufferedImage(16, height, BufferedImage.TYPE_INT_ARGB);
                for (int m = 0; m < 16; m++) {
                    for (int n = 0; n < 16; n++) {
                        image.setRGB(m, startY + n, globalImage.getRGB(m / 2 + xPos, n / 2 + yPos));
                    }
                }
                return image;
            }
        } catch (IOException | IllegalArgumentException | NullPointerException e) {
            e.printStackTrace();
            return charImage;
        }
    }

    @Nullable
    private FontChar getFontChar(int charAt) {
        if (chars[charAt] == null) {
			Minecraft mc = Minecraft.getMinecraft();
            if (mc.isCallingFromMinecraftThread()) {
                chars[charAt] = new FontChar((char) charAt, generateCharImage(charAt));
            } else {
                mc.addScheduledTask(() -> {
                    chars[charAt] = new FontChar((char) charAt, generateCharImage(charAt));
                });
            }
        }
        return chars[charAt];
    }

    public void refresh() {
        for (int i = 0; i < 32; i++)
            chars[i] = null;
        for (int i = 127; i < 65535; i++)
            chars[i] = null;
    }
}
