import * as React from 'react';
import * as ReactRedux from 'react-redux';

import {
  bindActionCreators
} from 'redux';

import {
  toast,
} from 'react-toastify';

import {
  Card,
  CardBody,
  Col,
  Row,
} from 'reactstrap';

import {
  FormattedTime
} from 'react-intl';

import {
  UPDATE_INTERVAL_SECONDS,
} from '../../model';

import {
  fetchAccounts,
  resetFilters,
  setFilter,
  setPager,
  setSelected,
  updateAccount,
} from '../../ducks/ui/views/account';

import {
  ToastTemplate,
} from '../helpers';

import {
  Filters,
  User,
  Users,
} from "./account";


/**
 * Browse and manage user accounts
 *
 * @class UserManager
 * @extends {React.Component}
 */
class UserManager extends React.Component {

  constructor(props) {
    super(props);

    this.refreshIntervalId = null;
  }

  componentWillMount() {
    this.refreshIntervalId = setInterval(() => {
      this.search();
    }, UPDATE_INTERVAL_SECONDS * 1000);

    this.search();
  }

  componentWillUnmount() {
    if (this.refreshIntervalId) {
      clearInterval(this.refreshIntervalId);
      this.refreshIntervalId = null;
    }
  }

  search() {
    this.props.fetchAccounts({
      query: { ...this.props.filters },
    });
  }

  updateAccount(account) {
    this.props.updateAccount(account)
      .then(() => {
        toast.dismiss();
        toast.success(
          <ToastTemplate iconClass='fa-user' text='Account has been updated successfully' />
        );

        this.search();
      })
      .catch((err) => {
        const reason = err.errors.map((e) => e.description + '<br>').join('');
        toast.dismiss();
        toast.error(
          <ToastTemplate
            iconClass='fa-user'
            html={`Account update has failed. Reason${err.errors.length > 1 ? 's' : ''}:<br>${reason} `} />
        );
      });
  }

  render() {
    const { items, selected } = this.props;
    const selectedUser = selected ? items.find((e) => e.id === selected.id) : null;

    return (
      <div className="animated fadeIn">
        <Row>
          <Col className="col-12">
            <Card>
              <CardBody className="card-body">
                {this.props.lastUpdate &&
                  <Row className="mb-2">
                    <Col >
                      <div className="small text-muted">
                        Last Update: <FormattedTime value={this.props.lastUpdate} day='numeric' month='numeric' year='numeric' />
                      </div>
                    </Col>
                  </Row>
                }
                <Row>
                  <Col>
                    <Filters
                      filters={this.props.filters}
                      setFilter={this.props.setFilter}
                      resetFilters={this.props.resetFilters}
                      fetchAccounts={this.props.fetchAccounts}
                    />
                  </Col>
                </Row>
              </CardBody>
            </Card>
            <Card>
              <CardBody className="card-body">
                <Row className="mb-2">
                  <Col>
                    <Users
                      fetchAccounts={this.props.fetchAccounts}
                      filters={this.props.filters}
                      items={this.props.items}
                      pager={this.props.pager}
                      selected={this.props.selected}
                      setPager={this.props.setPager}
                      setSelected={this.props.setSelected}
                    />
                  </Col>
                </Row>
              </CardBody>
            </Card>
          </Col>
        </Row >
        {selectedUser &&
          <Row>
            <Col className="col-12">
              <Card>
                <CardBody className="card-body">
                  <User
                    user={selectedUser}
                    onSave={(account) => this.updateAccount(account)}
                  />
                </CardBody>
              </Card>
            </Col>
          </Row>
        }
      </div>
    );
  }

}

const mapStateToProps = (state) => ({
  filters: state.ui.views.account.filters,
  items: state.ui.views.account.items,
  lastUpdate: state.ui.views.account.lastUpdate,
  loading: state.ui.views.account.loading,
  pager: state.ui.views.account.pager,
  selected: state.ui.views.account.selected,
});

const mapDispatchToProps = (dispatch) => bindActionCreators({
  fetchAccounts,
  resetFilters,
  setFilter,
  setPager,
  setSelected,
  updateAccount
}, dispatch);

export default ReactRedux.connect(mapStateToProps, mapDispatchToProps)(UserManager);
