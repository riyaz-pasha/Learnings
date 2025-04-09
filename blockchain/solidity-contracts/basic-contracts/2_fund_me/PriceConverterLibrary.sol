// SPDX-License-Identifier: MIT

pragma solidity ^0.8.0;

// import {AggregatorV3Interface} from "@chainlink/contracts/src/v0.8/shared/interfaces/AggregatorV3Interface.sol";
import "./AggregatorV3Interface.sol";
// https://github.com/smartcontractkit/chainlink/blob/develop/contracts/src/v0.8/shared/interfaces/AggregatorV3Interface.sol

library PriceConverterLibrary {
    function getPrice() internal view returns (uint256) {
        // ABI
        // Address ETH / USD 0x694AA1769357215DE4FAC081bf1f309aDC325306

        AggregatorV3Interface priceFeed = AggregatorV3Interface(
            0x694AA1769357215DE4FAC081bf1f309aDC325306
        );
        (, int256 price, , , ) = priceFeed.latestRoundData();
        // Assume 1 eth price in usd is 3030914000000
        // and feed has 8 decimal places in output,
        // so final value is 30309.1400 0000

        // 1 eth is = 1 * 10^18 wei
        // 1 eth = 30309.1400 0000 usd
        //
        return uint256(price * 1e10);
        // return uint256(price);
    }

    function getVersion() internal view returns (uint256) {
        AggregatorV3Interface priceFeed = AggregatorV3Interface(
            0x694AA1769357215DE4FAC081bf1f309aDC325306
        );
        return priceFeed.version();
    }

    function getConversionRate(
        uint256 ethAmount
    ) internal view returns (uint256) {
        uint256 ethPrice = getPrice();
        uint256 ethAmountInUsd = (ethPrice * ethAmount) / 1e18;
        return ethAmountInUsd;
    }
}
