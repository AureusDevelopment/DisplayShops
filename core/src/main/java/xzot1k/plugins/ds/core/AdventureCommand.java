package xzot1k.plugins.ds.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import xzot1k.plugins.ds.DisplayShops;

import java.util.List;

public class AdventureCommand {


    public static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.legacySection();

    public static final LegacyComponentSerializer oldSerializer = LegacyComponentSerializer.legacyAmpersand().toBuilder().hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private static final MiniMessage mm = MiniMessage.miniMessage();

    private static Component getAuthors() {
        String authorFormat = "<click:open_url:\"https://github.com/<author>\"><hover:show_text:\"<aqua>Click to see <yellow><author>'s</yellow> Github</aqua>\"><author></hover></click>";
        Component authors = Component.empty();
        int i = 0;
        List<String> authors1 = DisplayShops.getPluginInstance().getDescription().getAuthors();
        for (String author : authors1) {
            authors = authors.append(mm.deserialize(authorFormat, Placeholder.parsed("author", author)));
            if (i != authors1.size())
                authors = authors.append(Component.text(", "));
            i++;
        }
        return authors;
    }

    public static void runInfoAdventure(CommandSender commandSender) {
        DisplayShops INSTANCE = DisplayShops.getPluginInstance();
        String pluginVersion = (INSTANCE.getDescription().getVersion().toLowerCase().contains("build") ? "<red>" : "<green>") + INSTANCE.getDescription().getVersion();
        /*Pair<String, String> versionData = INSTANCE.getLatestVersion();*/
        String releasedVersion = (INSTANCE.getDescription().getVersion().toLowerCase().contains("snapshot") ? "<dark_blue>" : "<green>") + /*versionData.getKey()*/INSTANCE.getDescription().getVersion();

        MiniMessage specialMM = MiniMessage.builder().tags(TagResolver.builder().resolvers(StandardTags.newline(), StandardTags.color()).build()).build();
        Component releasedVersionComponent = mm.deserialize(releasedVersion).appendSpace().append(Component.text("Changelog (Hover)", NamedTextColor.RED).hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(specialMM.deserializeOr("<gold>" + INSTANCE.getDescription().getVersion().replace("\\n", "<br>"), Component.text(INSTANCE.getDescription().getVersion(), NamedTextColor.GOLD)))));


        String message = "\n<yellow><<st>------------</st>[ <aqua>DisplayShops</aqua> ]<st>------------</st>>\n" +
                "<gray>Current Plugin Version:</gray> <plversion>\n" +
                "<gray>Latest Released Version:</gray> <green><plreleasedver></green>\n" +
                "<gray>Author(s):</gray><aqua> <authors>\n" +
                "<yellow><<st>-------------------------------------</st>>";


        commandSender.sendMessage(mm.deserialize(message
                , Placeholder.parsed("plversion", pluginVersion)
                , Placeholder.component("plreleasedver", releasedVersionComponent)
                , Placeholder.component("authors", AdventureCommand::getAuthors)));
    }

}
