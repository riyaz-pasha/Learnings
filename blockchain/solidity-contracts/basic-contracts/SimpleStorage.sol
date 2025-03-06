// SPDX-License-Identifier: MIT
// pragma solidity 0.8.7;
pragma solidity 0.8.28;

contract SimpleStorage {
    // The concept of “undefined” or “null” values does not exist in Solidity, but newly declared variables always have a default value dependent on its type.
    // Value types -> bool, int, uint, address
    // int(int256) -> int(8*[1..32]) -> int8, int16, int24, int32, int40 ... int256

    uint256 storedData;
    mapping (string => uint) public nameToFavoriteNumber;
    People[] public people;

    struct People{
        uint256 favoriteNumber;
        string name;        
    }

    function store(uint256 _storedData) public  {
        storedData=_storedData;
    }

    function retrieveStoredData() public view returns(uint) {
        return  storedData;
    }

    function addPerson(string memory _name, uint256 _favoriteNumber) public{
        // people.push(People({favoriteNumber:_favoriteNumber,name:_name})); 
        people.push(People(_favoriteNumber, _name)); // passed in the order of declaration
        nameToFavoriteNumber[_name]=_favoriteNumber;
    }
}