package core.content;

import com.fasterxml.jackson.annotation.JsonValue;

/// Базовый класс контента в игре
/// Весь контент идентифицируется по уникальным для каждого базового типа
/// строковым идентификатором [#key()], но для эффективности чаще применяется
/// автоматически присвоенный [#id()], у которого нет гарантий на то, что
/// у каждой единицы контента будет всегда один числовой идентификатор
///
/// @implSpec Реализации [Object#equals(Object)], [Object#hashCode()] должны полностью полагаться на [#key()]
public interface ContentType {
    @JsonValue
    String key();

    short id();
    void setId(short id);
}
