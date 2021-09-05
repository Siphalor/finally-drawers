package de.siphalor.finallydrawers.client;

import de.siphalor.finallydrawers.block.DrawerBlock;
import de.siphalor.finallydrawers.block.DrawerBlockEntity;
import de.siphalor.finallydrawers.storage.DrawerStorage;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Vec3f;

public class DrawerBlockEntityRenderer extends BlockEntityRenderer<DrawerBlockEntity> {

	private final ItemRenderer itemRenderer;
	private final TextRenderer textRenderer;

	public DrawerBlockEntityRenderer(BlockEntityRenderDispatcher dispatcher) {
		super(dispatcher);
		itemRenderer = MinecraftClient.getInstance().getItemRenderer();
		textRenderer = MinecraftClient.getInstance().textRenderer;
	}

	@Override
	public void render(DrawerBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
		BlockState state = entity.getCachedState();
		Direction facing = state.get(DrawerBlock.FACING);
		BlockPos frontPos = entity.getPos().offset(facing);
		if (entity.getWorld().getBlockState(frontPos).isOpaqueFullCube(entity.getWorld(), frontPos)) {
			return;
		}
		light = WorldRenderer.getLightmapCoordinates(entity.getWorld(), frontPos);

		matrices.push();
		matrices.translate(0.5D, 0.5D, 0.5D);
		matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-facing.asRotation()));
		matrices.translate(-0.5D, 0.5D, 0.45D);

		DrawerBlock block = ((DrawerBlock) state.getBlock());
		DrawerStorage storage = entity.getStorage();

		final double Y_PARTIAL = 1D / block.getSlotGridHeight();
		final double X_PARTIAL = 1D / block.getSlotGridWidth();
		final float SCALE = (float) Math.min(Y_PARTIAL, X_PARTIAL) * 0.35F;
		matrices.translate(0D, -Y_PARTIAL * 0.5D, 0D);
		int k = 0;
		int color = (int) (light / 15728880F * 0xff);
		int shadowColor = (int) (color * 0.3F);
		color = color | color << 8 | color << 16;
		shadowColor = shadowColor | shadowColor << 8 | shadowColor << 16;
		for (int i = 0; i < block.getSlotGridHeight(); i++) {
			matrices.push();
			matrices.translate(X_PARTIAL * 0.5D, 0D, 0D);
			for (int j = 0; j < block.getSlotGridWidth(); j++) {
				DrawerStorage.Entry entry = storage.getEntry(k);

				matrices.push();

				matrices.scale(SCALE, SCALE, SCALE);
				ItemStack stack = entry.getReference();
				boolean depth = itemRenderer.getModels().getModel(stack).hasDepth();
				matrices.push();
				if (depth) {
					matrices.scale(1F, 1F, 0.001F);
					matrices.translate(0D, 0D, 0.1F);
					DiffuseLighting.enableGuiDepthLighting();
				} else {
					DiffuseLighting.disableGuiDepthLighting();
				}
				matrices.peek().getNormal().load(Matrix3f.scale(1, 1, 0.6F));
				itemRenderer.renderItem(entry.getReference(),
						ModelTransformation.Mode.GUI, light,
						OverlayTexture.DEFAULT_UV, matrices, vertexConsumers
				);
				DiffuseLighting.disableGuiDepthLighting();
				matrices.pop();
				DiffuseLighting.disable();

				matrices.translate(0D, 0D, -0.03D);
				matrices.scale(0.035F, -0.035F, -0.035F);

				if (!entry.isEmpty()) {
					String text;
					if (entry.getStackMaxCount() == 1) {
						text = String.valueOf(entry.getAmount());
					} else {
						int stacks = entry.getAmount() / entry.getStackMaxCount();
						if (stacks > 0) {
							text = stacks + "×" + entry.getStackMaxCount() + " + " + (entry.getAmount() % entry.getStackMaxCount());
						} else {
							text = String.valueOf(entry.getAmount());
						}
					}
					textRenderer.draw(
							matrices, new LiteralText(text), -textRenderer.getWidth(text) / 2F, 20, color
					);
					matrices.translate(0D, 0D, 0.001D);
					textRenderer.draw(
							matrices, new LiteralText(text), -textRenderer.getWidth(text) / 2F + 1, 21, shadowColor
					);
				}
				matrices.pop();
				matrices.translate(X_PARTIAL, 0D, 0D);
				k++;
			}
			matrices.pop();
			matrices.translate(0D, -Y_PARTIAL, 0D);
		}

		matrices.pop();
	}
}
