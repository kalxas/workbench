import * as React from 'react';
import * as ReactRedux from 'react-redux';

import classnames from 'classnames';

import {
  bindActionCreators,
} from 'redux';

import {
  Col,
  Row,
} from 'reactstrap';

import {
  EnumInputType,
  EnumSelection,
} from '../../../../model/process-designer';

import {
  filterResource,
  filteredResources,
  removeResourceFromBag,
  setActiveResource,
} from '../../../../ducks/ui/views/process-designer';

import {
  ProcessInput,
} from '../../process/designer';

/**
 * A connected component for rendering resources available to the resource
 * export wizard.
 *
 * @class Sidebar
 * @extends {React.Component}
 */
class Sidebar extends React.Component {

  /**
   * Resolves selected item
   *
   * @returns the selected item
   * @memberof Sidebar
   */
  getSelectedItem() {
    switch (this.props.active.type) {
      case EnumSelection.Resource:
        return this.props.resources.find((resource) => {
          return ((resource.key === this.props.active.item) && (resource.inputType === EnumInputType.CATALOG));
        }) || null;
      default:
        return null;
    }
  }

  /**
   * Renders a single {@link ProcessInput}.
   *
   * @param {any} resource
   * @returns a {@link ProcessInput} component instance
   * @memberof Sidebar
   */
  renderResource(resource) {
    return (
      <ProcessInput
        key={resource.key}
        resource={resource}
        remove={this.props.removeResourceFromBag}
        setActiveResource={this.props.setActiveResource}
        active={this.props.active.type === EnumSelection.Resource && this.props.active.item === resource.key}
      />
    );
  }

  render() {
    return (
      <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <Row className="slipo-re-sidebar-resource-list-wrapper">
          <Col className="slipo-re-sidebar-resource-list-wrapper">
            <div style={{ borderBottom: '1px solid #cfd8dc', padding: '0.7rem' }}>
              Resource Bag
            </div>
            <div className={
              classnames({
                "slipo-re-sidebar-resource-list": true,
                "slipo-re-sidebar-resource-list-empty": (this.props.resources.length === 0),
              })
            }>
              {this.props.resources.length > 0 &&
                this.props.resources
                  .filter((r) => r.inputType === EnumInputType.CATALOG)
                  .map((r) => this.renderResource(r))
              }
              {this.props.resources.length === 0 &&
                <div className="text-muted slipo-pd-tip" style={{ paddingLeft: 1 }}>No resources selected</div>
              }
            </div>
          </Col>
        </Row>
      </div >
    );
  }
}

const mapStateToProps = (state) => ({
  active: state.ui.views.process.designer.active,
  process: state.ui.views.process.designer.process,
  steps: state.ui.views.process.designer.steps,
  resources: filteredResources(state.ui.views.process.designer),
  filters: state.ui.views.process.designer.filters,
});

const mapDispatchToProps = (dispatch) => bindActionCreators({
  filterResource,
  removeResourceFromBag,
  setActiveResource,
}, dispatch);

const mergeProps = (stateProps, dispatchProps, ownProps) => {
  return {
    ...stateProps,
    ...dispatchProps,
    ...ownProps,
  };
};

export default ReactRedux.connect(mapStateToProps, mapDispatchToProps, mergeProps)(Sidebar);
