function debounce(func, delay) {
    let timeout;
    return function (...args) {
        clearTimeout(timeout);
        timeout = setTimeout(() => {
            func.apply(this, args);
        }, delay);
    }
}

function search(query) {
    console.log(`Searching for : ${query}`);
}

const debounceSearch = debounce(search, 500);

debounceSearch("H")
debounceSearch("He")
debounceSearch("Hel")
debounceSearch("Hell")
debounceSearch("Hello");
