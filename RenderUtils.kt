package cn.asone.endless.utils

import net.minecraft.client.renderer.GlStateManager

object RenderUtils {
    @JvmStatic
    fun quickGLColor(hex: Int) {
        val alpha = (hex shr 24 and 0xFF) / 255F
        val red = (hex shr 16 and 0xFF) / 255F
        val green = (hex shr 8 and 0xFF) / 255F
        val blue = (hex and 0xFF) / 255F
        GlStateManager.color(red, green, blue, alpha)
    }
}