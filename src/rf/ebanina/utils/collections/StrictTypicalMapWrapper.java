package rf.ebanina.utils.collections;

import java.util.Objects;
import java.util.Set;

public class StrictTypicalMapWrapper<K>
    extends TypicalMapWrapper<K>
{
    private final Set<Class<?>> allowedTypes;

    public StrictTypicalMapWrapper(Class<?>... allowedTypes) {
        super();

        if (allowedTypes == null || allowedTypes.length == 0) {
            throw new IllegalArgumentException("allowedTypes cannot be empty");
        }

        this.allowedTypes = Set.of(allowedTypes);
    }

    @Override
    public <T> void put(K key, T value, Class<T> type) {
        validateType(type);

        super.put(key, value, type);
    }

    @Override
    public <T> void putAuto(K key, T value) {
        throw new UnsupportedOperationException(
                "putAuto() disabled in StrictTypicalMapWrapper. Use put(key, value, type) with allowed type."
        );
    }

    private void validateType(Class<?> type) {
        if (!allowedTypes.contains(type)) {
            throw new IllegalArgumentException(
                    "Type %s not allowed. Allowed: %s"
                            .formatted(type.getSimpleName(),
                                    allowedTypes.stream().map(Class::getSimpleName).toList())
            );
        }
    }

    public Set<Class<?>> getAllowedTypes() {
        return allowedTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StrictTypicalMapWrapper<?> that)) return false;
        return super.equals(o) &&
                Objects.equals(allowedTypes, ((StrictTypicalMapWrapper<?>) o).allowedTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), allowedTypes);
    }

    @Override
    public String toString() {
        return "StrictTypicalMapWrapper{" +
                "allowedTypes=" + allowedTypes.stream().map(Class::getSimpleName).toList() +
                "} " + super.toString();
    }
}
