import json

for name in ['Maps/tutorial.ldtk', 'Maps/final_map.ldtk']:
    path = f'assets/{name}'
    with open(path, 'r') as f:
        data = json.load(f)
    
    print(f'\n=== {name} ===')
    
    # Check IntGrid layer definitions
    for layer_def in data.get('defs', {}).get('layers', []):
        if layer_def.get('__type') == 'IntGrid':
            print(f'  IntGrid layer: "{layer_def["identifier"]}"')
            for val in layer_def.get('intGridValues', []):
                print(f'    Value {val["value"]}: "{val.get("identifier", "unnamed")}"')
    
    # Check each level for lava entities and intgrid usage
    for i, level in enumerate(data.get('levels', [])):
        lava_found = False
        for layer in level.get('layerInstances', []):
            ltype = layer.get('__type', '')
            lid = layer.get('__identifier', '')
            
            # Check for LavaDamage entities
            if ltype == 'Entities':
                for ent in layer.get('entityInstances', []):
                    if 'lava' in ent.get('__identifier', '').lower():
                        print(f'  Level {i}: Entity "{ent["__identifier"]}" at px={ent["px"]}')
                        lava_found = True
            
            # Check IntGrid for non-zero values and identify what layer they're in
            if ltype == 'IntGrid':
                csv = layer.get('intGridCsv', [])
                unique_vals = set(v for v in csv if v != 0)
                if unique_vals:
                    print(f'  Level {i}: IntGrid "{lid}" has values: {sorted(unique_vals)}')
                    lava_found = True
        
        if not lava_found:
            pass  # Don't print anything for levels with no lava
