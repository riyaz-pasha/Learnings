// SPDX-License-Identifier: MIT

pragma solidity ^0.8.0;

import "./PriceConverterLibrary.sol";

// oracle problem
// using chain link

contract FundMe {
    using PriceConverterLibrary for uint256;

    uint256 public minUSD = 50 * 1e18;
    address[] public funders;
    mapping(address => uint256) public addressToAmountFunded;
    address public owner;

    constructor() {
        owner = msg.sender;
    }

    modifier onlyOwner() {
        // _; // execute statements first then check
        require(msg.sender == owner, "Sender is not owner!.");
        _; // execute statements after the check
    }

    function fund() public payable {
        // 1e18 = 1 * (10 ** 18) = 1 * ( 100 0000 0000 0000 0000 wei)
        require(
            msg.value.getConversinRate() >= minUSD,
            "Didn't send enough ETH!"
        );
        funders.push(msg.sender);
        addressToAmountFunded[msg.sender] = msg.value;
    }

    function withdraw() public onlyOwner {
        for (
            uint256 funderIndex = 0;
            funderIndex < funders.length;
            funderIndex++
        ) {
            address funder = funders[funderIndex];
            addressToAmountFunded[funder] = 0;
        }

        // reset
        funders = new address[](0);

        // withdraw

        // transfer
        // payable(msg.sender).transfer(address(this).balance);

        // send
        // bool sendSuccess = payable(msg.sender).send(address(this).balance);
        // require(sendSuccess, "send failed");

        // call
        // (bool callSuccess, bytes memory dataReturned) = payable(msg.sender)
        (bool callSuccess, ) = payable(msg.sender).call{
            value: address(this).balance
        }("");
        require(callSuccess, "Call failed");
    }
}
