/**
 * TermIt
 * Copyright (C) 2019 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.termit.exception.UnsupportedOperationException;
import cz.cvut.kbss.termit.rest.dto.AssetType;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import java.net.URI;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/identifiers")
public class IdentifierController extends BaseController {

    @Autowired
    public IdentifierController(IdentifierResolver identifierResolver, Configuration config) {
        super(identifierResolver, config);
    }

    /**
     * Returns identifier which would be generated by the application for the specified asset name (using the
     * configured namespace).
     *
     * @param name Vocabulary name
     * @return Generated vocabulary identifier
     */
    @PreAuthorize("permitAll()")
    @PostMapping
    public URI generateIdentifier(@RequestParam("name") String name,
                                  @RequestParam(value = "vocabularyIri", required = false) String vocabularyIri,
                                  @RequestParam("assetType") AssetType assetType) {
        if (assetType == null) {
            throw new UnsupportedOperationException("No asset type supplied.");
        }

        switch (assetType) {
            case TERM:
                Objects.requireNonNull(vocabularyIri);
                return idResolver.generateDerivedIdentifier(URI.create(vocabularyIri),ConfigParam.TERM_NAMESPACE_SEPARATOR, name);
            case VOCABULARY:
                return idResolver.generateIdentifier(ConfigParam.NAMESPACE_VOCABULARY, name);
            case RESOURCE:
                return idResolver.generateIdentifier(ConfigParam.NAMESPACE_RESOURCE, name);
            default:
                throw new UnsupportedOperationException("Unsupported asset type " + assetType + " supplied.");
        }
    }
}
