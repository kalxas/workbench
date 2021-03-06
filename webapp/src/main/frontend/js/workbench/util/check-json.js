import _ from 'lodash';

import {
  ServerError,
} from '../model/error';

export function checkError(r) {
  if (_.isEmpty(r.errors)) {
    return r;
  } else {
    throw new ServerError(r.errors);
  }
}
