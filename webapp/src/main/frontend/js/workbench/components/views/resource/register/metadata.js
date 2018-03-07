import React from 'react';
import { SelectField, TextField } from '../../../helpers/forms/form-fields/';

const supportedFiles = [
  { value: 'SHAPEFILE' },
  { value: 'CSV' },
  { value: 'GPX' },
  { value: 'GEOJSON' },
  { value: 'XML' },
  { value: 'OSM' },
];

export const initialValue = {
  name: '',
  description: '',
  format: null,
};

export const validator = (value) => {
  const errors = {};

  if (!value.name) {
    errors.name = 'File name required';
  }
  if (!value.description || value.description.length < 5) {
    errors.description = 'Description should be longer than 5 characters';
  }

  if (Object.keys(errors).length) {
    throw errors;
  }
};

export const Component = (props) => {
  return (
    <div>
      <TextField
        {...props}
        id="name"
        label="Resource name"
        help="Resource name"
      />
      <TextField
        {...props}
        id="description"
        label="Resource description"
        help="Resource description"
      />
    </div>
  );
};
