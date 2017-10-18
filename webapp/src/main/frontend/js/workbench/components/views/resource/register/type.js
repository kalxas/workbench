import React from 'react';
import { SelectField } from '../../../helpers/forms/wizard-fields/';

const options = [
  { value: 'UPLOAD', label: 'File upload' },
  { value: 'EXISTING', label: 'Resource list' },
  { value: 'EXTERNAL', label: 'External url' },
  { value: 'HARVESTER', label: 'Harvester' },
];

export const initialValue = { 
  path: options[0], 
};

export const Component = (props) => {
  return (
    <div>
      <SelectField
        {...props}
        id="path"
        label="Data source"
        help="Choose one"
        options={options}
      />
    </div>
  );
};