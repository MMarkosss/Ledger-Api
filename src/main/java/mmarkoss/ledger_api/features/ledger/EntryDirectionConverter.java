package mmarkoss.ledger_api.features.ledger;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EntryDirectionConverter implements AttributeConverter<EntryDirection, String> {

    @Override
    public String convertToDatabaseColumn(EntryDirection attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public EntryDirection convertToEntityAttribute(String dbData) {
        return dbData == null ? null : EntryDirection.fromCode(dbData);
    }
}
