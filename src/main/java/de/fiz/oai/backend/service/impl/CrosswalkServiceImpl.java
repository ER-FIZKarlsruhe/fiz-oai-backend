/*
 * Copyright 2019 FIZ Karlsruhe - Leibniz-Institut fuer Informationsinfrastruktur GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fiz.oai.backend.service.impl;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;

import de.fiz.oai.backend.dao.DAOCrosswalk;
import de.fiz.oai.backend.exceptions.AlreadyExistsException;
import de.fiz.oai.backend.exceptions.NotFoundException;
import de.fiz.oai.backend.models.Crosswalk;
import de.fiz.oai.backend.models.Format;
import de.fiz.oai.backend.service.CrosswalkService;
import de.fiz.oai.backend.service.FormatService;

@Service
public class CrosswalkServiceImpl implements CrosswalkService {

	@Inject
	DAOCrosswalk daoCrosswalk;

	@Inject
	FormatService formatService;

	@Override
	public Crosswalk read(String name) throws IOException {
		Crosswalk crosswalk = daoCrosswalk.read(name);
		return crosswalk;
	}

	@Override
	public Crosswalk create(Crosswalk crosswalk) throws IOException {
		// Does the crosswalk already exists?
		Crosswalk oldCrosswalk = daoCrosswalk.read(crosswalk.getName());
		if (oldCrosswalk != null) {
			throw new AlreadyExistsException("Crosswalk with name " + crosswalk.getName() + " already exist.");
		}

		// Does the from format (referenced by crosswalk) exists?
		Format from = formatService.read(crosswalk.getFormatFrom());
		if (from == null) {
			throw new NotFoundException("Format from " + crosswalk.getFormatFrom() + " not found.");
		}

		// Does the to format (referenced by crosswalk) exists?
		Format to = formatService.read(crosswalk.getFormatTo());
		if (to == null) {
			throw new NotFoundException("Forma to " + crosswalk.getFormatTo() + " not found.");
		}

		Crosswalk newCrosswalk = daoCrosswalk.create(crosswalk);
		return newCrosswalk;
	}

	@Override
	public Crosswalk update(Crosswalk crosswalk) throws IOException {
		// Does the format (referenced by crosswalk) exists?
		Crosswalk oldCrosswalk = daoCrosswalk.read(crosswalk.getName());
		if (oldCrosswalk == null) {
			throw new NotFoundException("Crosswalk with name " + crosswalk.getName() + " not found.");
		}

		// Does the format (referenced by crosswalk) exists?
		Format from = formatService.read(crosswalk.getFormatFrom());
		if (from == null) {
			throw new NotFoundException("Format from " + crosswalk.getFormatFrom() + " not found.");
		}

		// Does the format (referenced by crosswalk) exists?
		Format to = formatService.read(crosswalk.getFormatTo());
		if (to == null) {
			throw new NotFoundException("Forma to " + crosswalk.getFormatTo() + " not found.");
		}

		daoCrosswalk.delete(crosswalk.getName());
		return daoCrosswalk.create(crosswalk);
	}

	@Override
	public List<Crosswalk> readAll() throws IOException {
		List<Crosswalk> crosswalks = daoCrosswalk.readAll();

		return crosswalks;
	}

	@Override
	public void delete(String name) throws IOException {
		daoCrosswalk.delete(name);
	}

}
