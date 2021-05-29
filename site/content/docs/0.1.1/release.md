---
content_type: "release"
version_name: "0.1.1"
latest: true
has_notes: false
has_javadoc: false
published: true
---
* Annotations are renamed to Meta. 
* Deprecated and removed Items from meta package and replaced by immutable lists.
* TimeValue produces Instant instead of LocalDateTime since Instant is absolute while LocalDateTime is not.