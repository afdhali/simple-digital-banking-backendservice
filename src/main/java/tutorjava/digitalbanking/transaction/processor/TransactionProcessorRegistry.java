package tutorjava.digitalbanking.transaction.processor;

import org.springframework.stereotype.Component;
import tutorjava.digitalbanking.transaction.TransactionType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** FACTORY / registry: resolves the strategy for a type. Spring injects every TransactionProcessor bean. */
@Component
public class TransactionProcessorRegistry {

    private final Map<TransactionType, TransactionProcessor> processors = new EnumMap<>(TransactionType.class);

    public TransactionProcessorRegistry(List<TransactionProcessor> available) {
        for (TransactionProcessor processor : available) {
            TransactionProcessor previous = processors.put(processor.supportedType(), processor);
            if (previous != null) {
                throw new IllegalStateException("Duplicate processor for " + processor.supportedType());
            }
        }
        for (TransactionType type : TransactionType.values()) {
            if (!processors.containsKey(type)) {
                throw new IllegalStateException("No processor registered for " + type);
            }
        }
    }

    public TransactionProcessor get(TransactionType type) {
        return processors.get(type);
    }
}
