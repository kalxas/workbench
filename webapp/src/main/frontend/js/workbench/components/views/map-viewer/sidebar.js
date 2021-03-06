import * as React from 'react';
import * as ReactRedux from 'react-redux';

import classnames from 'classnames';

import {
  bindActionCreators
} from 'redux';

import {
  Col,
  Nav,
  NavItem,
  NavLink,
  Row,
  TabContent,
  TabPane,
} from 'reactstrap';

import {
  Layer,
} from './';

import {
  SelectField,
} from '../../helpers/forms/fields';

import {
  EnumLayerType,
} from '../../../model/map-viewer';

import {
  selectLayer,
  setBaseLayer,
  toggleLayer,
  toggleLayerConfiguration,
} from '../../../ducks/ui/views/map-viewer';

import {
  RevisionHistory,
} from './';

/**
 * A connected component for rendering execution selected files available to map
 * viewer
 *
 * @class Sidebar
 * @extends {React.Component}
 */
class Sidebar extends React.Component {

  constructor(props) {
    super(props);

    this.toggle = this.toggle.bind(this);

    this.state = {
      activeTab: '1',
    };
  }

  get supportedBaseLayers() {
    const baseLayers = [
      { value: 'OSM', label: 'Open Street Maps' },
    ];

    if (this.props.bingMaps.applicationKey) {
      baseLayers.push({ value: 'BingMaps-Road', label: 'Bing Maps (Road)' });
      baseLayers.push({ value: 'BingMaps-Aerial', label: 'Bing Maps (Aerial)' });
    }

    return baseLayers;
  }

  toggle(tab) {
    if (this.state.activeTab !== tab) {
      this.setState({
        activeTab: tab
      });
    }
  }

  /**
   * Renders a single {@link Layer}.
   *
   * @param {any} layer
   * @returns a {@link Layer} component instance
   * @memberof Sidebar
   */
  renderLayer(layer) {
    return (
      <Layer
        key={`${layer.tableName}-${layer.color}`}
        layer={layer}
        toggle={this.props.toggleLayer}
        select={this.props.selectLayer}
        selected={this.props.selectedLayer !== null && this.props.selectedLayer.tableName === layer.tableName}
        toggleLayerConfiguration={this.props.toggleLayerConfiguration}
      />
    );
  }

  render() {
    const inputLayers = this.props.layers.filter((l) => l.type === EnumLayerType.Input);
    const outputLayers = this.props.layers.filter((l) => l.type === EnumLayerType.Output);

    return (
      <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <Nav tabs style={{ height: '2.75rem' }}>
          <NavItem>
            <NavLink className={classnames({ active: this.state.activeTab === '1' })}
              onClick={() => { this.toggle('1'); }}>
              <i className="icon-layers"></i>
              {this.state.activeTab === '1' &&
                <div style={{ margin: '-2px 0px 0px 6px', float: 'right' }}> Layers</div>
              }
            </NavLink>
          </NavItem>
          <NavItem>
            <NavLink className={classnames({ active: this.state.activeTab === '2' })}
              onClick={() => { this.toggle('2'); }}>
              <i className="icon-settings"></i>
              {this.state.activeTab === '2' &&
                <div style={{ margin: '-2px 0px 0px 6px', float: 'right' }}> Settings</div>
              }
            </NavLink>
          </NavItem>
        </Nav>
        <TabContent activeTab={this.state.activeTab}>
          <TabPane tabId="1">
            <Row>
              <Col>
                <div style={{ borderBottom: '1px solid #cfd8dc', padding: 11 }}>
                  Input
                </div>
                <div className="slipo-pd-sidebar-map-layer-list">
                  {inputLayers.length > 0 &&
                    inputLayers.map((l) => this.renderLayer(l))
                  }
                  {inputLayers.length === 0 &&
                    <div className="text-muted slipo-pd-tip" style={{ paddingLeft: 1 }}>No input layers found</div>
                  }
                </div>
                <div style={{ borderBottom: '1px solid #cfd8dc', borderTop: '1px solid #cfd8dc', padding: 11 }}>
                  Output
                </div>
                <div className="slipo-pd-sidebar-map-layer-list">
                  {outputLayers.length > 0 &&
                    outputLayers.map((l) => this.renderLayer(l))
                  }
                  {outputLayers.length === 0 &&
                    <div className="text-muted slipo-pd-tip" style={{ paddingLeft: 1 }}>No output layers found</div>
                  }
                </div>
              </Col>
            </Row>
          </TabPane>
          <TabPane tabId="2">
            <Row>
              <Col>
                <div className="slipo-pd-sidebar-map-settings">
                  <SelectField
                    id="baseLayer"
                    label="Base Layer"
                    value={this.props.baseLayer || 'OSM'}
                    onChange={(value) => this.props.setBaseLayer(value)}
                    options={this.supportedBaseLayers}
                  />
                </div>
              </Col>
            </Row>
          </TabPane>
        </TabContent>
        <RevisionHistory resource={this.props.data.resource} version={this.props.data.version} />
      </div >
    );
  }

}

const mapStateToProps = (state) => ({
  baseLayer: state.ui.views.map.config.baseLayer,
  bingMaps: state.config.bingMaps,
  data: state.ui.views.map.data,
  layers: state.ui.views.map.config.layers,
  osm: state.config.osm,
  selectedFeatures: state.ui.views.map.config.selectedFeatures,
  selectedLayer: state.ui.views.map.config.selectedLayer,
});

const mapDispatchToProps = (dispatch) => bindActionCreators({
  selectLayer,
  setBaseLayer,
  toggleLayer,
  toggleLayerConfiguration,
}, dispatch);

const mergeProps = (stateProps, dispatchProps, ownProps) => {
  return {
    ...stateProps,
    ...dispatchProps,
    ...ownProps,
  };
};

export default ReactRedux.connect(mapStateToProps, mapDispatchToProps, mergeProps)(Sidebar);
