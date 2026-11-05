import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { CartProvider } from './context/CartContext';
import { Layout } from './components/Layout';
import { LekiList } from './components/LekiList';
import { LekDetails } from './components/LekDetails';
import { CartPage } from './components/CartPage';

function App() {
  return (
    <CartProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/" element={<LekiList />} />
            <Route path="/leki/:id" element={<LekDetails />} />
            <Route path="/koszyk" element={<CartPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </CartProvider>
  );
}

export default App;