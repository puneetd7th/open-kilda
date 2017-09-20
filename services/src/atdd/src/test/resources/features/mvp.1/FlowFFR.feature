@FFR
Feature: Flow failover, failure and recovery

  @MVP1.1
  Scenario: Failover followed by failure followed by recovery

  This scenario checks that failover and recovery happens orderly and that
  failures do not break things apart.

    Given a clean flow topology
    And a clean controller
    And basic multi-path topology
    And topology contains 12 links
    And a flow is successfully created
    And traffic flows through this flow

    When a route in use fails
    And there is an alternative route
    Then traffic flows through this flow

    When a route in use fails
    And there is no alternative route
    Then traffic does not flow through this flow

    When a failed route comes back up
    Then traffic flows through this flow

  @MVP1.1
  Scenario: ISL failover followed by failure followed by recovery

    Given a clean flow topology
    And a clean controller
    And basic multi-path topology
    And topology contains 12 links
    And a flow is successfully created
    And traffic flows through this flow

    When an ISL in use fails
    And there is an alternative route
    Then traffic flows through this flow

    When an ISL in use fails
    And there is no alternative route
    Then traffic does not flow through this flow

    When a failed ISL comes back up
    Then traffic flows through this flow

  @MVP1.1
  Scenario: Switch failover followed by failure followed by recovery

    Given a clean flow topology
    And a clean controller
    And basic multi-path topology
    And topology contains 12 links
    And a flow is successfully created
    And traffic flows through this flow

    When a switch in use fails
    And there is an alternative route
    Then traffic flows through this flow

    When a switch in use fails
    And there is no alternative route
    Then traffic does not flow through this flow

    When a failed switch comes back up
    Then traffic flows through this flow
