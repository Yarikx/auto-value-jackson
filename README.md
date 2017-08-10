# AutoValue: Jackson Extension

Extension to generate serializers/deserializers for AutoValue classes.

### Roadmap

* [x] \[de\]serialize simple classes
* [x] \[de\]serialize properties with type arguments
* [x] Support custom json names via `@JsonProperty`
* [x] Polymorphic \[de\]serialization
* [x] Ability to a add default values
* [x] Custom \[de\]serializers via `@JsonSerialize`/`@JsonDeserialize` 
* [ ] Support `@JsonIgnore`
* [ ] Resolve properties \[de\]serializers once
* [x] Classes with generic parameters
* [x] Nice error messages