package repulica.customcommands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import dev.hbeck.kdl.objects.*;
import dev.hbeck.kdl.parse.KDLParser;
import io.github.cottonmc.staticdata.StaticData;
import io.github.cottonmc.staticdata.StaticDataItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class CustomCommands implements ModInitializer {

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(((dispatcher, dedicated) -> {
			KDLParser parser = new KDLParser();
			Set<StaticDataItem> data = StaticData.getAll("commands.kdl");
			for (StaticDataItem item : data) {
				try {
					KDLDocument doc = parser.parse(item.createInputStream());
					for (KDLNode node : doc.getNodes()) {
						if (!node.getIdentifier().equals("literal")) {
							throw new IllegalArgumentException("custom commands: top-level commands must be literal");
						}
						LiteralArgumentBuilder<ServerCommandSource> base = CommandManager.literal(node.getArgs().get(0).getAsString().getValue());
						parseNode(dispatcher, node, base);
						dispatcher.register(base);
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}));
	}

	private <T extends ArgumentBuilder<ServerCommandSource, T>> void parseNode(CommandDispatcher<ServerCommandSource> dispatcher, KDLNode node, ArgumentBuilder<ServerCommandSource, T> builder) {
		if (node.getChild().isPresent()) {
			for (KDLNode child : node.getChild().get().getNodes()) {
				switch (child.getIdentifier()) {
					case "literal":
						LiteralArgumentBuilder<ServerCommandSource> litBase = CommandManager.literal(child.getArgs().get(0).getAsString().getValue());
						parseNode(dispatcher, child, litBase);
						builder.then(litBase);
						break;
					//TODO: implement somehow
//					case "argument":
//						String name = node.getArgs().get(0).getAsString().getValue();
//						String rawArgType = node.getArgs().get(1).getAsString().getValue();
//						//TODO: min/max for double/float/int/long arg types and all the minecraft specific argument types
//						ArgumentType<?> type = switch (rawArgType) {
//							case "boolean" -> BoolArgumentType.bool();
//							case "double" -> DoubleArgumentType.doubleArg();
//							case "float" -> FloatArgumentType.floatArg();
//							case "int" -> IntegerArgumentType.integer();
//							case "long" -> LongArgumentType.longArg();
//							case "word" -> StringArgumentType.word();
//							case "string" -> StringArgumentType.string();
//							case "greedyString" -> StringArgumentType.greedyString();
//							case "blockState" -> BlockStateArgumentType.blockState();
//							case "entity" -> EntityArgumentType.entity();
//							case "entities" -> EntityArgumentType.entities();
//							case "player" -> EntityArgumentType.player();
//							case "players" -> EntityArgumentType.players();
//							case "itemStack" -> ItemStackArgumentType.itemStack();
//						};
//						RequiredArgumentBuilder<ServerCommandSource, ?> argBase = CommandManager.argument(name, type);
//						parseNode(dispatcher, child, argBase);
//						builder.then(argBase);
//						break;
					case "requires":
						Predicate<ServerCommandSource> reqs = source -> true; //TODO: change when theres more than one `requires` option
						Map<String, KDLValue> props = child.getProps();
						for (String key : props.keySet()) {
							KDLValue val = props.get(key);
							//TODO: other common requirements from permissions libs or stuff
							switch (key) {
								case "permissionLevel":
									KDLNumber trueVal = val.getAsNumber().orElse(KDLNumber.from(Integer.MAX_VALUE));
									int processedVal = trueVal.getAsBigDecimal().intValue();
									reqs = source -> source.hasPermissionLevel(processedVal);
									break;
							}
						}
						builder.requires(reqs);
						break;
					case "executes":
						//TODO: arguments somehow
						String command = child.getArgs().get(0).getAsString().getValue();
						builder.executes(context -> dispatcher.execute(command, context.getSource()));
						break;
				}
			}
		}
	}
}
