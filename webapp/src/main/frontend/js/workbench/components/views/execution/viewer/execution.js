import * as React from 'react';
import PropTypes from 'prop-types';

import moment from '../../../../moment-localized';

import {
  FormattedTime
} from 'react-intl';

import {
  Button,
  Card,
  CardBody,
  Col,
  Row,
} from 'reactstrap';

import {
  JobStatus,
} from '../../../helpers';

class Execution extends React.Component {

  constructor(props) {
    super(props);
  }

  static propTypes = {
    execution: PropTypes.object.isRequired,
    viewMap: PropTypes.func.isRequired,
  };

  render() {
    const e = this.props.execution;

    return (
      <Card>
        <CardBody>
          <Row className="mb-2">
            <Col>
              <div className="font-weight-bold mb-1">Name</div>
              <div className="font-weight-italic">{e.name}</div>
            </Col>
            <Col>
              <div className="font-weight-bold mb-1">Submitted By</div>
              <div className="font-weight-italic">{e.submittedBy ? e.submittedBy.name : '-'}</div>
            </Col>
            <Col>
              <div className="font-weight-bold mb-1">Submitted On</div>
              <div className="font-weight-italic"><FormattedTime value={e.submittedOn} day='numeric' month='numeric' year='numeric' /></div>
            </Col>
            <Col>
              <div className="font-weight-bold mb-1">Status</div>
              <JobStatus status={e.status} />
            </Col>
          </Row>
          <Row className="mb-2">
            <Col>
              <div className="font-weight-bold mb-1">Started On</div>
              <div className="font-weight-italic">
                {e.startedOn ?
                  <FormattedTime value={e.startedOn} day='numeric' month='numeric' year='numeric' />
                  :
                  '-'
                }
              </div>
            </Col>
            <Col>
              <div className="font-weight-bold mb-1">Completed On</div>
              <div className="font-weight-italic">
                {e.completedOn ?
                  <FormattedTime value={e.completedOn} day='numeric' month='numeric' year='numeric' />
                  :
                  '-'
                }
              </div>
            </Col>
            <Col>
              <div className="font-weight-bold mb-1">Duration</div>
              <div className="font-weight-italic">
                {e.completedOn ?
                  moment.duration(e.completedOn - e.startedOn).humanize()
                  :
                  moment.duration(Date.now() - e.startedOn).humanize()
                }
              </div>
            </Col>
            <Col>
              <div className="font-weight-bold mb-1">Error message</div>
              {e.errorMessage ?
                e.errorMessage
                :
                '-'
              }
            </Col>
          </Row>
          {e.exported &&
            <Row>
              <Col>
                <div className="float-right">
                  <Button color="primary" onClick={this.props.viewMap} className="float-left"><i className="fa fa-map-o mr-1"></i> View Map</Button>
                </div>
              </Col>
            </Row>
          }
        </CardBody>
      </Card>
    );
  }

}

export default Execution;
