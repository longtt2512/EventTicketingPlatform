# Architecture diagram icons

These 21 original icons illustrate components of the Event Ticketing Platform.
The SVG files retain editable vector geometry. PlantUML embeds the matching
transparent PNG files; no remote image downloads are required.

`build_icons.py` generates both formats from the same geometry using Python
and Pillow. Run it from this directory:

```sh
python build_icons.py
```

From the repository root, render the diagrams sequentially so shared image
assets load consistently:

```sh
plantuml -tpng -charset UTF-8 docs/technical-system-assets/architecture.puml
plantuml -tpng -charset UTF-8 docs/technical-system-assets/service-boundaries.puml
plantuml -tpng -charset UTF-8 docs/technical-system-assets/commerce-components.puml
```

The illustrations identify component roles. They are not vendor logos or a
claim that additional infrastructure exists. The diagrams describe the
implemented system and use the report's service names and ownership boundaries.
