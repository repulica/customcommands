# Custom Commands

custom command aliases for vanillish servers written in [kdl](https://kdl.dev)

custom commands lets server owners write a file at `static_data/commands.kdl` to add aliases or shorthands for special commands or triggers

currently requires adding a built jar of [kdl4j 0.1.0](https://github.com/hkolbeck/kdl4j/packages/582497) at `libs/kdl4j-0.1.0.jar` because github package registry is broken

current supported nodes:
- `literal <name>` - keyword command node
- `requires [requirements]` - requirements for parent node, properties determine requirements:
  - `permissionLevel` - required permission level to run command
- `executes <command>` - sets what command the parent node executes

to do:
- [ ] allow arbitrary arguments instead of just keywords
- [ ] add more properties for requirements

sample configuration:
```kdl
literal "afk" { //shorthand for afk data pack https://www.planetminecraft.com/data-pack/afk-pack/
    executes "trigger afk"
}
literal "giveall" { //give preset items to everyone on the server
    requires permissionLevel=4
    literal "diamonds" { //escaped quotes
        executes "give @a diamond{display:{Name:'[{\"text\":\"happy diamond\",\"italic\":false}]'}} 64"
    }
    literal "magicsword" { //kdl raw strings
        executes r#"give @p netherite_sword{Unbreakable:1,display:{Name:'[{"text":"magic sword","italic":false}]',Lore:['[{"text":"a sword passed down","italic":false}]','[{"text":"through the ages","italic":false}]','[{"text":"from hero to hero","italic":false}]']},Enchantments:[{id:fire_aspect,lvl:2},{id:knockback,lvl:2},{id:looting,lvl:3},{id:sharpness,lvl:5}]} 64"#
    }
}
```
