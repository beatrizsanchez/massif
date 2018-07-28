/*******************************************************************************
 * Copyright (c) 2010-2013, Embraer S.A., Budapest University of Technology and Economics
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *
 * Contributors: 
 *     Marton Bur, Abel Hegedus, Akos Horvath - initial API and implementation 
 *******************************************************************************/
package hu.bme.mit.massif.simulink.api.adapter.block;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;

import hu.bme.mit.massif.communication.command.MatlabCommand;
import hu.bme.mit.massif.communication.command.MatlabCommandFactory;
import hu.bme.mit.massif.communication.datatype.CellMatlabData;
import hu.bme.mit.massif.communication.datatype.Handle;
import hu.bme.mit.massif.communication.datatype.IVisitableMatlabData;
import hu.bme.mit.massif.communication.datatype.MatlabString;
import hu.bme.mit.massif.communication.datatype.StructMatlabData;
import hu.bme.mit.massif.simulink.Block;
import hu.bme.mit.massif.simulink.Property;
import hu.bme.mit.massif.simulink.PropertyType;
import hu.bme.mit.massif.simulink.SimulinkFactory;
import hu.bme.mit.massif.simulink.SimulinkReference;
import hu.bme.mit.massif.simulink.api.Importer;

/**
 * Generic adapter for non-specific blocks. This adapter is used when no adapter is registered for a block type. The
 * process method should be called from every class subclassing it.
 */
public class DefaultBlockAdapter implements IBlockAdapter {

    @Override
    public Block getBlock(Importer traverser) {
        return SimulinkFactory.eINSTANCE.createBlock();
    }

    @Override
    public void process(Importer traverser, SimulinkReference parentSimRef, Block blockToProcess) {
        // The default block processing implementation goes here

        MatlabCommandFactory commandFactory = traverser.getCommandFactory();
        String blockFQN = blockToProcess.getSimulinkRef().getFQN();
        
        List<Property> blockProperties = new LinkedList<Property>();

        MatlabCommand getAllBlockParameters = commandFactory.customCommand("get_all_block_parameters", 1).addParam(blockFQN);
        Map<String, IVisitableMatlabData> blockPropsMap = StructMatlabData.getStructMatlabDataData(getAllBlockParameters.execute());
        
        Set<Entry<String, IVisitableMatlabData>> entries = blockPropsMap.entrySet();
        for (Entry<String, IVisitableMatlabData> entry : entries) {
			IVisitableMatlabData value = entry.getValue();
			
			Property prop = SimulinkFactory.eINSTANCE.createProperty();
			prop.setName(entry.getKey());
			
			if (value == null) {
				// Default: empty string
				prop.setType(PropertyType.STRING_PROPERTY);
				prop.setValue("");				
				blockProperties.add(prop);	
			} else { 
				if(value instanceof MatlabString) {
					prop.setType(PropertyType.STRING_PROPERTY);
					prop.setValue(value.toString());				
				} else if(value instanceof Handle) {
					prop.setType(PropertyType.DOUBLE_PROPERTY);
					prop.setValue(value.toString());				
				} else if (value instanceof CellMatlabData) {
					prop.setType(PropertyType.STRING_PROPERTY);
					CellMatlabData cell = (CellMatlabData) value;
					StringJoiner joiner = new StringJoiner(",", "[", "]");
					for (IVisitableMatlabData data : cell.getDatas()) {
						joiner.add(data.toString());
					}
					prop.setValue(joiner.toString());
				} else if(value instanceof StructMatlabData) {
					prop.setType(PropertyType.STRING_PROPERTY);
					StructMatlabData struct = (StructMatlabData) value;
					StringJoiner joiner = new StringJoiner(",", "{", "}");
					for (Entry<String, IVisitableMatlabData> structEntry : struct.getDatas().entrySet()) {
						joiner.add(structEntry.getKey().toString() + ":" + (structEntry.getValue() == null ? "" : structEntry.getValue().toString()));
					}
					prop.setValue(joiner.toString());
				}
				blockProperties.add(prop);
			} 
        }
        
        blockToProcess.getProperties().addAll(blockProperties);

    }

}
