package com.relyon.credflow.configuration;

import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import com.relyon.credflow.model.descriptionmapping.DescriptionMappingRequestDTO;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

    @Bean
    public ModelMapper modelMapper() {
        var mm = new ModelMapper();

        Converter<Long, Category> idToCategory = ctx -> {
            var id = ctx.getSource();
            if (id == null) return null;
            var c = new Category();
            c.setId(id);
            return c;
        };

        mm.typeMap(DescriptionMappingRequestDTO.class, DescriptionMapping.class)
                .addMappings(m -> m.using(idToCategory)
                        .map(DescriptionMappingRequestDTO::getCategoryId, DescriptionMapping::setCategory));

        return mm;
    }
}