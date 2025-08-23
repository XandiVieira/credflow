package com.relyon.credflow.configuration;

import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import com.relyon.credflow.model.descriptionmapping.DescriptionMappingRequestDTO;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.transaction.TransactionRequestDTO;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        var mm = new ModelMapper();

        mm.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
                .setSkipNullEnabled(true);

        Converter<Long, Category> idToCategory = ctx -> {
            var id = ctx.getSource();
            if (id == null) return null;
            var c = new Category();
            c.setId(id);
            return c;
        };

        mm.typeMap(TransactionRequestDTO.class, Transaction.class)
                .addMappings(m -> {
                    m.skip(Transaction::setId);
                    m.using(idToCategory).map(TransactionRequestDTO::getCategoryId, Transaction::setCategory);
                });

        mm.typeMap(DescriptionMappingRequestDTO.class, DescriptionMapping.class)
                .addMappings(m -> {
                    m.skip(DescriptionMapping::setId);
                    m.using(idToCategory).map(DescriptionMappingRequestDTO::getCategoryId, DescriptionMapping::setCategory);
                });

        return mm;
    }
}