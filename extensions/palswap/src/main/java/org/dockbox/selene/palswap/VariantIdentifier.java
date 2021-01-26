package org.dockbox.selene.palswap;

import org.dockbox.selene.core.files.FileManager;
import org.dockbox.selene.core.i18n.common.Language;
import org.dockbox.selene.core.objects.Exceptional;
import org.dockbox.selene.core.objects.item.Item;
import org.dockbox.selene.core.server.Selene;
import org.dockbox.selene.core.server.bootstrap.SeleneBootstrap;
import org.dockbox.selene.core.util.SeleneUtils;
import org.dockbox.selene.palswap.fileparsers.BlockRegistryParser;
import org.dockbox.selene.structures.registry.RegistryIdentifier;
import org.dockbox.selene.palswap.fileparsers.ItemData;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum VariantIdentifier implements RegistryIdentifier {
    FULL("log"),
    STAIRS,
    SLAB,
    WALL,
    FENCE,
    FENCE_GATE,
    TRAPDOOR("trapdoormodel, woodenboardfull"),
    CARPET,
    PANE,
    IRONBAR,
    VERTICAL_SLAB,
    CORNER_BLOCK("corner"),
    LAYER("snowlayer", "snow"),
    ARCH,
    SMALL_ARCH,
    PILLAR,
    SMALL_PILLAR,
    NEWEL_CAP("dragonegg", "fullpartial"),
    BALUSTRADE("anvil"),
    CAPITAL("hopperfull"),
    CAPITAL_DIRECTIONAL("hopperdirectional"),
    WINDOW_SLIT("slit", "fullslit"),
    ARROW_SLIT("windowslit"),
    VERTICAL_BEAM("beam"),
    HORIZONTAL_BEAM("beamhorizontal"),
    ROCKS("rocks", "rock");


    private final Set<String> identifiers;
    private static final Map<String, VariantIdentifier> identifierMap = SeleneUtils.emptyConcurrentMap();
    private static BlockRegistryParser blockRegistryParser;
    private static ItemData overridenBlockNames;

    public static final Pattern blockNameIdentifierRegex = Pattern.compile("(?:\\w* )*(\\w* )+(\\w*)");
    public static final Pattern conquestIdIdentifierRegex = Pattern.compile("(?:conquest:)?\\w*_(\\w*)_\\d*(?::\\d{1,2})?");

    static {
        for (VariantIdentifier variantIdentifier : values()) {
            for (String identifier : variantIdentifier.identifiers) {
                identifierMap.put(identifier, variantIdentifier);
            }
        }
        if (SeleneBootstrap.isConstructed()) {
            blockRegistryParser = Selene.provide(BlockRegistryParser.class);
        }
    }

    public static ItemData getOverridenBlockNames() {
        if (null == overridenBlockNames) {
            if (!SeleneBootstrap.isConstructed()) return (overridenBlockNames = new ItemData());

            FileManager fm = Selene.provide(FileManager.class);
            Path file = fm.getDataFile(BlockRegistryExtension.class, "overridenblocknames");

            Exceptional<ItemData> mappings = fm.read(file, ItemData.class);
            mappings.ifPresent(m -> overridenBlockNames = m)
                    .ifAbsent(() -> overridenBlockNames = new ItemData());
        }
        return overridenBlockNames;
    }

    VariantIdentifier(String... identifier) {
        this.identifiers = new HashSet<>(Arrays.asList(identifier));
        this.identifiers.add(this.name().toLowerCase().replaceAll("_", " "));
        this.identifiers.add(this.name().toLowerCase().replaceAll("_", ""));
    }

    VariantIdentifier() {
        this.identifiers = new HashSet<>();
        this.identifiers.add(this.name().toLowerCase().replaceAll("_", ""));
        this.identifiers.add(this.name().toLowerCase().replaceAll("_", " "));
    }

    public static Exceptional<VariantIdentifier> of(String identifier) {
        identifier = identifier.toLowerCase();

        return identifierMap.containsKey(identifier)
                ? Exceptional.of(identifierMap.get(identifier))
                : Exceptional.empty();
    }

    public static Exceptional<VariantIdentifier> ofItem(Item item) {
        return ofID(item.getId());
    }

    public static String getBlockNameWithoutVariant(String name) {
        name = getOverridenBlockNames().getItemRegistry().getOrDefault(name.replace(" ", "_"), name);

        Matcher matcher = blockNameIdentifierRegex.matcher(prepareForMatcher(name));

        if (matcher.matches()) {
            String lastWord = matcher.group(2);
            String secondLastWord = matcher.group(1);

            if (of(secondLastWord + lastWord).isPresent())
                return name.replace(" " + secondLastWord + lastWord, "").trim();
            if (of(lastWord).isPresent())
                return name.replace(lastWord, "").trim();
            if (name.split(" ")[0].equalsIgnoreCase("horizontal") && lastWord.equalsIgnoreCase("beam"))
                return name.replace("Horizontal", "").trim();
        }

        return name.trim();
    }

    public static Exceptional<VariantIdentifier> ofName(String name) {
        name = getOverridenBlockNames().getItemRegistry().getOrDefault(name.replace(" ", "_"), name);

        Matcher matcher = blockNameIdentifierRegex.matcher(prepareForMatcher(name));
        Exceptional<VariantIdentifier> variant = Exceptional.empty();

        if (matcher.matches()) {
            variant = of(matcher.group(1) + matcher.group(2));

            if (variant.isAbsent())
                variant = of(matcher.group(2));
            if (variant.isAbsent() && matcher.group(2).equalsIgnoreCase("beam"))
                if(name.split(" ")[0].equalsIgnoreCase("horizontal"))
                    return Exceptional.of(VariantIdentifier.HORIZONTAL_BEAM);
                //The vertical beam and full block have the same name.
                else return Exceptional.of(new IllegalArgumentException(
                    String.format("The name %s, is an ambigous variant of %s and %s. It is recommended to use ofID instead.",
                            name, VariantIdentifier.FULL, VariantIdentifier.VERTICAL_BEAM)));
        }

        if (variant.isAbsent() && !BlockIdentifier.ofName(name).isAir())
            return Exceptional.of(VariantIdentifier.FULL);

        return variant;
    }

    public static Exceptional<VariantIdentifier> ofID(String id) {
        return of(id.substring(id.lastIndexOf("_") + 1));
    }

    @Deprecated
    public static Exceptional<VariantIdentifier> ofRawItem(Item item) {
        String id = item.getId();
        if (id.contains("minecraft:")) {
            return ofName(item.getDisplayName(Language.EN_US).toStringValue());
        }
        return ofRawID(id);
    }

    @Deprecated
    public static Exceptional<VariantIdentifier> ofRawID(String id) {
        //Minecraft ID:
        if (id.contains("minecraft:")) {
            return ofName(blockRegistryParser.getItemFromRawID(id).getDisplayName(Language.EN_US).toStringValue());
        }
        //Conquest ID:
        else {
            Matcher matcher = conquestIdIdentifierRegex.matcher(id);

            if (matcher.matches()) {
                return of(matcher.group(1));
            }
        }
        return Exceptional.empty();
    }

    public static String prepareForMatcher(String name) {
        return name
                .replace("^", "UP_ARROW")
                .replace("/", "FORWARD_SLASH")
                .replace("\\", "BACK_SLASH")
                .replace("(", "")
                .replace(")", "")
                .replace("-", "_")
                .replace(" Vertical Connection", "")
                .replace(" CTM Round Top", "")
                .replace(" CTM Elongated", "");
    }
}
